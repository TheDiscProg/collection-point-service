package simex.collectionpoint.domain.caching
import simex.caching.CachingServiceAlgebra
import simex.messaging.Simex

class ResponseCachingService[F[_]](
    cachingMap: Map[String, CachingServiceAlgebra[F]],
    defaultCache: CachingServiceAlgebra[F]
) extends ResponseCachingServiceAlgebra[F] {

  override def save(key: String, message: Simex): F[Unit] =
    message.endpoint.entity match {
      case Some(entity) => saveMessageInCache(key, entity, message)
      case None => defaultCache.saveMessage(key, message)
    }

  override def getResponse(request: Simex): F[Option[Simex]] = {
    val key = s"${request.client.clientId}-${request.client.requestId}"
    request.endpoint.entity match {
      case Some(entity) => getCachedMessage(key, entity)
      case None => defaultCache.getMessage(key)
    }
  }

  private def saveMessageInCache(key: String, entity: String, message: Simex): F[Unit] =
    cachingMap.getOrElse(entity, defaultCache).saveMessage(key, message)

  private def getCachedMessage(key: String, entity: String): F[Option[Simex]] =
    cachingMap.getOrElse(entity, defaultCache).getMessage(key)
}

object ResponseCachingService {

  def apply[F[_]](
      cachingMap: Map[String, CachingServiceAlgebra[F]],
      defaultCache: CachingServiceAlgebra[F]
  ) =
    new ResponseCachingService[F](cachingMap, defaultCache)
}
