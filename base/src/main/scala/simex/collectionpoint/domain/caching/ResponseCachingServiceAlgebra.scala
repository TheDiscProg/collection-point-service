package simex.collectionpoint.domain.caching

import simex.messaging.Simex

trait ResponseCachingServiceAlgebra[F[_]] {

  def save(key: String, message: Simex): F[Unit]

  def getResponse(request: Simex): F[Option[Simex]]

}
