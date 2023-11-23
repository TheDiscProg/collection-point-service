package simex.collectionpoint.domain.orchestrator

import simex.messaging.Simex

trait CollectionPointMessageHandlerAlgebra[F[_]] {

  def handleSimexMessage(message: Simex): F[Unit]

  def getResponse(request: Simex): F[Option[Simex]]
}
