package simex.collectionpoint.domain.caching

import cats.Applicative
import org.typelevel.log4cats.Logger
import shareprice.entity.EntitySecurity
import simex.caching.{CachingService, CachingServiceAlgebra}
import simex.caching.config.HazelcastConfig

import scala.annotation.tailrec

object EntityCacheMapper {

  def getEntityCacheMap[F[_]: Applicative: Logger](
      hzConfig: HazelcastConfig
  ): Map[String, CachingServiceAlgebra[F]] = {

    @tailrec
    def loop(
        entities: List[EntitySecurity],
        cacheMap: Map[String, CachingServiceAlgebra[F]]
    ): Map[String, CachingServiceAlgebra[F]] =
      entities match {
        case head :: tail =>
          loop(
            tail,
            cacheMap + (head.value -> CachingService(
              hzConfig.copy(authTokenTTL = head.ttl),
              head.value
            ))
          )
        case Nil => cacheMap
      }

    loop(EntitySecurity.values.toList, Map())
  }

  def getDefaultCache[F[_]: Applicative: Logger](
      hzConfig: HazelcastConfig
  ): CachingServiceAlgebra[F] =
    CachingService(
      hzConfig.copy(authTokenTTL = EntitySecurity.Default.ttl),
      EntitySecurity.Default.value
    )
}
