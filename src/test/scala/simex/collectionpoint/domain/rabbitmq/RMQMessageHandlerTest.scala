package simex.collectionpoint.domain.rabbitmq

import cats.effect.IO
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import simex.collectionpoint.domain.DefaultTestSetting
import simex.collectionpoint.domain.orchestrator.CollectionPointMessageHandlerAlgebra
import simex.messaging.Simex
import cats.effect.unsafe.implicits.global

class RMQMessageHandlerTest extends AnyFlatSpec with Matchers with DefaultTestSetting {

  val orc = new CollectionPointMessageHandlerAlgebra[IO] {
    override def handleSimexMessage(message: Simex): IO[Unit] = IO(())

    override def getResponse(request: Simex): IO[Option[Simex]] = IO(None)
  }

  val sut = new RMQMessageHandler[IO](orc)

  it should "pass the message received to the orchestrator" in {
    val result = sut.handleReceivedMessage(responseA).unsafeToFuture()

    whenReady(result) { r =>
      r shouldBe ()
    }
  }
}
