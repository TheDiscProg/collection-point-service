package simex.collectionpoint.domain.caching

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.OptionValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import shareprice.entity.EntitySecurity
import simex.collectionpoint.domain.DefaultTestSetting

class CacheMapperAndServiceTest
    extends AnyFlatSpec
    with Matchers
    with OptionValues
    with HazelcastContainer
    with DefaultTestSetting {

  val hzCast = setUpHCastContainer()
  val config = hzCast._1

  val defaultCache = EntityCacheMapper.getDefaultCache[IO](config)
  val cacheMap = EntityCacheMapper.getEntityCacheMap[IO](config)
  val sut = ResponseCachingService[IO](cacheMap, defaultCache)

  "The Entity mapper" should "give maps of entity with it's respective caching service" in {
    cacheMap.size shouldBe 4
    EntitySecurity.values.map(entity => cacheMap.keySet.contains(entity.value)).toSet shouldBe Set(
      true
    )
  }

  it should "save and retrieve authorisation by client and request IDs with basic security" in {
    val result = (for {
      _ <- sut.save(s"$clientA-$request1", clientAAuthResponse)
      _ <- sut.save(s"$clientB-$request1", clientBAuthResponse)
      auth <- sut.getResponse(authRequest)
    } yield auth).unsafeToFuture()

    whenReady(result) { r =>
      r.isDefined shouldBe true
      r.value shouldBe clientAAuthResponse
    }
  }

  it should "return a None when the ID was not found" in {
    val result = sut
      .getResponse(authRequest.copy(client = authRequest.client.copy(requestId = "request4")))
      .unsafeToFuture()

    whenReady(result) { r =>
      r.isDefined shouldBe false
    }
  }

  it should "store a message in the default cache when entity is not defined" in {
    val aResponse = responseA.copy(endpoint = responseA.endpoint.copy(entity = None))
    val aRequest = authRequest.copy(
      endpoint = authRequest.endpoint.copy(entity = None),
      client = authRequest.client.copy(requestId = request3)
    )
    val key = s"${aResponse.originator.clientId}-${aResponse.originator.requestId}"

    val result = (for {
      _ <- sut.save(key, aResponse)
      fromDefault <- defaultCache.getMessage(key)
      fromMapper <- sut.getResponse(aRequest)
    } yield (fromDefault, fromMapper)).unsafeToFuture()

    whenReady(result) { tupple =>
      tupple._1.isDefined shouldBe true
      tupple._1.value shouldBe aResponse
      tupple._2.isDefined shouldBe true
      tupple._2.value shouldBe aResponse
    }
  }
}
