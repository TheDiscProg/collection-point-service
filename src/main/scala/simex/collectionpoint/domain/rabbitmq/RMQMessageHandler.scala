package simex.collectionpoint.domain.rabbitmq

import cats.Monad
import org.typelevel.log4cats.Logger
import simex.collectionpoint.domain.orchestrator.CollectionPointMessageHandlerAlgebra
import simex.messaging.Simex
import cats.syntax.all._

class RMQMessageHandler[F[_]: Monad: Logger](orc: CollectionPointMessageHandlerAlgebra[F]) {

  def handleReceivedMessage(msg: Simex): F[Unit] =
    for {
      _ <- Logger[F].info(
        s"Message Handler received for ${msg.endpoint.resource}: ${msg.originator.clientId}/${msg.originator.requestId}"
      )
      _ <- orc.handleSimexMessage(msg)
    } yield ()
}
