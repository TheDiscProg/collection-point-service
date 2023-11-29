package simex.collectionpoint.domain.orchestrator

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.github.blemale.scaffeine.Scaffeine
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import simex.caching.CachingServiceAlgebra
import simex.collectionpoint.domain.DefaultTestSetting
import simex.collectionpoint.domain.caching.ResponseCachingServiceAlgebra
import simex.messaging.{Client, Method, Originator, Simex}
import thediscprog.utillibrary.caching.ScaffeineCachingService

class CollectionPointMessageServiceTest
    extends AnyFlatSpec
    with Matchers
    with OptionValues
    with BeforeAndAfterEach
    with DefaultTestSetting {

  private val cache = Scaffeine().build[String, Simex]()

  private val cachingService = new ScaffeineCachingService[IO](cache)

  override def beforeEach(): Unit = {
    cache.invalidateAll()
    super.beforeEach()
  }

  val responseCacheService = new ResponseCachingServiceAlgebra[IO] {

    override def save(key: String, message: Simex): IO[Unit] =
      cachingService.storeInCache(key, message).map(_ => ())

    override def getResponse(request: Simex): IO[Option[Simex]] = {
      val key = s"${request.client.clientId}-${request.client.requestId}"
      cachingService.getFromCache(key)
    }
  }

  val authTokenCacheService = new CachingServiceAlgebra[IO] {

    override def saveMessage(key: String, message: Simex): IO[Unit] =
      cachingService.storeInCache(key, message).map(_ => ())

    override def getMessage(key: String): IO[Option[Simex]] =
      cachingService.getFromCache(key)

    override def deleteMessage(key: String): IO[Unit] =
      throw new RuntimeException(s"Should not delete $key")
  }

  val sut = CollectionPointMessageService[IO](responseCacheService, authTokenCacheService)

  it should "save a response message" in {
    val result = (for {
      _ <- sut.handleSimexMessage(responseA)
      stored <- cachingService.getFromCache(
        s"${responseA.originator.clientId}-${responseA.originator.requestId}"
      )
    } yield stored).unsafeToFuture()

    whenReady(result) { r =>
      r.isDefined shouldBe true
      r.value shouldBe responseA
    }
  }

  it should "return None for a request with no matching request" in {
    val result = sut.getResponse(authRequest).unsafeToFuture()

    whenReady(result) { r =>
      r.isDefined shouldBe false
    }
  }

  it should "return the response when the request passes the basic checks" in {
    val request = authRequest.copy(
      endpoint = clientAAuthResponse.endpoint,
      client = authRequest.client.copy(
        clientId = clientAAuthResponse.originator.clientId,
        requestId = clientAAuthResponse.originator.requestId
      )
    )
    val result = (for {
      _ <- cachingService.storeInCache(
        s"${clientAAuthResponse.originator.clientId}-${clientAAuthResponse.originator.requestId}",
        clientAAuthResponse
      )
      res <- sut.getResponse(request)
    } yield res).unsafeToFuture()

    whenReady(result) { r =>
      r.isDefined shouldBe true
      r.value shouldBe clientAAuthResponse
    }
  }

  it should "return None when the request does not pass the basic checks" in {
    val request = authRequest.copy(
      endpoint = clientAAuthResponse.endpoint,
      client = authRequest.client.copy(
        clientId = clientAAuthResponse.originator.clientId,
        requestId = "request10"
      )
    )
    val result = (for {
      _ <- cachingService.storeInCache(
        s"${clientAAuthResponse.originator.clientId}-${clientAAuthResponse.originator.requestId}",
        clientAAuthResponse
      )
      res <- sut.getResponse(request)
    } yield res).unsafeToFuture()

    whenReady(result) { r =>
      r.isDefined shouldBe false
    }
  }

  it should "return response when authorization exists and the request has greater security" in {
    val request = Simex(
      endpoint = cpEndpoint.copy(method = Method.SELECT.value, entity = None),
      client = Client(
        clientId = clientA,
        requestId = request3,
        sourceEndpoint = "app.data",
        authorization = authToken1
      ),
      originator = Originator(
        clientId = clientA,
        requestId = request3,
        sourceEndpoint = "app.data",
        originalToken = "oldtoken",
        security = "2"
      ),
      data = Vector()
    )
    val key = s"${responseA.originator.clientId}-${responseA.originator.requestId}"

    val result = (for {
      _ <- cachingService.storeInCache(authToken1, clientAAuthResponse)
      _ <- cachingService.storeInCache(key, responseA)
      res <- sut.getResponse(request)
    } yield res).unsafeToFuture()

    whenReady(result) { r =>
      r.isDefined shouldBe true
      r.value shouldBe responseA
    }
  }

  it should "return None when authorization does not exist and request has greater security" in {
    val request = Simex(
      endpoint = cpEndpoint.copy(method = Method.SELECT.value, entity = None),
      client = Client(
        clientId = clientA,
        requestId = request3,
        sourceEndpoint = "app.data",
        authorization = "oldtoken"
      ),
      originator = Originator(
        clientId = clientA,
        requestId = request3,
        sourceEndpoint = "app.data",
        originalToken = "oldtoken",
        security = "2"
      ),
      data = Vector()
    )
    val key = s"${responseA.originator.clientId}-${responseA.originator.requestId}"

    val result = (for {
      _ <- cachingService.storeInCache(authToken1, clientAAuthResponse)
      _ <- cachingService.storeInCache(key, responseA)
      res <- sut.getResponse(request)
    } yield res).unsafeToFuture()

    whenReady(result) { r =>
      r.isDefined shouldBe false
    }
  }

  it should "return response for original token level security" in {
    val request = Simex(
      endpoint = cpEndpoint.copy(method = Method.SELECT.value, entity = None),
      client = Client(
        clientId = clientA,
        requestId = request3,
        sourceEndpoint = "app.data",
        authorization = authToken1
      ),
      originator = Originator(
        clientId = clientA,
        requestId = request3,
        sourceEndpoint = "app.data",
        originalToken = "oldtoken",
        security = "3"
      ),
      data = Vector()
    )
    val key = s"${responseA.originator.clientId}-${responseA.originator.requestId}"
    val response =
      responseA.copy(originator = responseA.originator.copy(originalToken = "oldtoken"))

    val result = (for {
      _ <- cachingService.storeInCache(authToken1, clientAAuthResponse)
      _ <- cachingService.storeInCache(key, response)
      res <- sut.getResponse(request)
    } yield res).unsafeToFuture()

    whenReady(result) { r =>
      r.isDefined shouldBe true
      r.value shouldBe response
    }
  }

  it should "return None when original token level security check does not pass" in {
    val request = Simex(
      endpoint = cpEndpoint.copy(method = Method.SELECT.value, entity = None),
      client = Client(
        clientId = clientA,
        requestId = request3,
        sourceEndpoint = "app.data",
        authorization = authToken1
      ),
      originator = Originator(
        clientId = clientA,
        requestId = request3,
        sourceEndpoint = "app.data",
        originalToken = "atoken",
        security = "3"
      ),
      data = Vector()
    )
    val key = s"${responseA.originator.clientId}-${responseA.originator.requestId}"
    val response =
      responseA.copy(originator = responseA.originator.copy(originalToken = "oldtoken"))

    val result = (for {
      _ <- cachingService.storeInCache(authToken1, clientAAuthResponse)
      _ <- cachingService.storeInCache(key, response)
      res <- sut.getResponse(request)
    } yield res).unsafeToFuture()

    whenReady(result) { r =>
      r.isDefined shouldBe false
    }
  }
}
