package simex.collectionpoint.domain

import cats.effect.IO
import org.scalatest.concurrent._
import org.scalatest.time.{Millis, Seconds, Span}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import shareprice.config.ServiceDefinition
import simex.guardrail.definitions.SimexMessage
import simex.guardrail.definitions.{EndPoint => EndPointMessage}
import simex.guardrail.definitions.{Client => ClientMessage}
import simex.guardrail.definitions.{Originator => OriginatorMessage}
import simex.messaging._

trait DefaultTestSetting extends ScalaFutures {

  implicit def unsafeLogger = Slf4jLogger.getLogger[IO]

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(30, Seconds), interval = Span(100, Millis))

  val clientA = "clientA"
  val clientB = "clientB"
  val request1 = "request1"
  val request2 = "request2"
  val request3 = "request3"
  val authToken1 = "token1"
  val refreshToken1 = "token2"
  val authToken2 = "token3"
  val refreshToken2 = "token3"

  val cpEndpoint = Endpoint(
    resource = ServiceDefinition.CollectionPointService,
    method = Method.RESPONSE.value,
    entity = Some(Simex.AUTHENTICATION_ENTITY)
  )

  val client = Client(
    clientId = ServiceDefinition.AuthenticationService,
    requestId = s"$clientA-$request1",
    sourceEndpoint = ServiceDefinition.AuthenticationService,
    authorization = "sometoken"
  )

  val originator = Originator(
    clientId = clientA,
    requestId = request1,
    sourceEndpoint = "app.auth",
    originalToken = "",
    security = Security.BASIC.level
  )

  val data = Vector(
    Datum.OkayStatus,
    Datum(field = "clientId", value = "1", check = None),
    Datum(field = "name", value = "John Smith", check = None),
    Datum(field = "username", value = "john", check = None),
    Datum(field = "authorization", value = authToken1, check = None),
    Datum(field = "refresh_token", value = refreshToken1, check = None)
  )

  val clientAAuthResponse =
    Simex(endpoint = cpEndpoint, client = client, originator = originator, data = data)

  val clientBAuthResponse = Simex(
    endpoint = cpEndpoint,
    client = client.copy(requestId = s"$clientB-$request1"),
    originator = originator.copy(clientId = clientB),
    data = Vector(
      Datum.OkayStatus,
      Datum(field = "clientId", value = "2", check = None),
      Datum(field = "name", value = "Jack Jones", check = None),
      Datum(field = "username", value = "Jack", check = None),
      Datum(field = "authorization", value = authToken2, check = None),
      Datum(field = "refresh_token", value = refreshToken2, check = None)
    )
  )

  val responseA = Simex(
    endpoint = cpEndpoint.copy(entity = None),
    client = Client(
      clientId = "service.cal",
      requestId = s"$clientA-$request2",
      sourceEndpoint = "service.cal",
      authorization = "sometoken"
    ),
    originator = originator.copy(requestId = request3, originalToken = authToken1),
    data = Vector(
      Datum.OkayStatus,
      Datum("something", "something", None)
    )
  )

  val authRequest = Simex(
    endpoint = cpEndpoint.copy(method = Method.SELECT.value),
    client = Client(
      clientId = clientA,
      requestId = request1,
      sourceEndpoint = "app.auth",
      authorization = ""
    ),
    originator = clientAAuthResponse.originator,
    data = Vector()
  )

  val httpRequest = SimexMessage(
    endpoint = EndPointMessage(
      resource = ServiceDefinition.CollectionPointService,
      method = Method.SELECT.value,
      entity = None
    ),
    client = ClientMessage(
      clientId = clientA,
      requestId = request1,
      sourceEndpoint = "app.cal",
      authorization = "sometoken"
    ),
    originator = OriginatorMessage(
      clientId = clientA,
      requestId = request1,
      sourceEndpoint = "app.cal",
      originalToken = "sometoken",
      security = Security.BASIC.level
    ),
    data = Vector()
  )
}
