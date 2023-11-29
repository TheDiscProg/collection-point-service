package simex.collectionpoint.domain.endpoint

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import simex.collectionpoint.domain.DefaultTestSetting
import simex.collectionpoint.domain.orchestrator.CollectionPointMessageHandlerAlgebra
import simex.guardrail.collectionpoint.CollectionpointResource.ColllectPointRequestResponse
import simex.messaging.Simex

class CollectionPointEndpointHandlerTest extends AnyFlatSpec with Matchers with DefaultTestSetting {

  val orc = new CollectionPointMessageHandlerAlgebra[IO] {

    override def handleSimexMessage(message: Simex): IO[Unit] = IO(())

    override def getResponse(request: Simex): IO[Option[Simex]] =
      IO {
        if (request.client.clientId == responseA.originator.clientId)
          Some(responseA)
        else
          None
      }
  }

  val sut = new CollectionPointEndpointHandler[IO](orc)

  it should "return no content when no response matches" in {
    val badRequest = httpRequest.copy(client = httpRequest.client.copy(clientId = clientB))

    val result = sut.colllectPointRequest(ColllectPointRequestResponse)(badRequest).unsafeToFuture()

    whenReady(result) { r =>
      r shouldBe ColllectPointRequestResponse.NoContent
    }
  }

  it should "return OK request when response is available" in {
    val result =
      sut.colllectPointRequest(ColllectPointRequestResponse)(httpRequest).unsafeToFuture()

    whenReady(result) { r =>
      r shouldBe ColllectPointRequestResponse.Ok(
        SimexMessgeTransformer.transformToResponse(responseA)
      )
    }
  }

}
