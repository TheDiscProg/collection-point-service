package simex

import cats.data.NonEmptyList
import cats.effect.std.Dispatcher
import cats.effect.{Async, Resource}
import cats.implicits.toSemigroupKOps
import cats.{Monad, MonadError, Parallel}
import com.comcast.ip4s._
import io.circe.config.parser
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.Logger
import org.typelevel.log4cats.{Logger => Log4CatsLogger}
import shareprice.caching.CachingMap
import shareprice.config.ServerConfiguration
import simex.caching.{CachingService, CachingServiceAlgebra}
import simex.collectionpoint.config.ServiceConfig
import simex.collectionpoint.domain.caching.{EntityCacheMapper, ResponseCachingService}
import simex.collectionpoint.domain.endpoint.CollectionPointEndpointHandler
import simex.collectionpoint.domain.orchestrator.CollectionPointMessageService
import simex.collectionpoint.domain.rabbitmq.{RMQMessageHandler, RMQMessageRouter}
import simex.guardrail.collectionpoint.CollectionpointResource
import simex.guardrail.healthcheck.HealthcheckResource
import simex.rabbitmq.Rabbit
import simex.rabbitmq.publisher.SimexMQPublisher
import simex.server.domain.healthcheck.{
  HealthCheckService,
  HealthChecker,
  HealthcheckAPIHandler,
  SelfHealthCheck
}
import simex.server.entities.AppService

object AppServer {

  def createServer[F[
      _
  ]: Monad: Async: Log4CatsLogger: Parallel: MonadError[*[_], Throwable]]()
      : Resource[F, AppService[F]] =
    for {
      conf <- Resource.eval(parser.decodePathF[F, ServerConfiguration](path = "server"))

      // RabbitMQ Client and publisher
      rmqDispatcher <- Dispatcher.parallel
      rmqClient <- Resource.eval(Rabbit.getRabbitClient(conf.rabbitMQ, rmqDispatcher))
      rmqPublisher = new SimexMQPublisher[F](rmqClient)

      // Hazelcast Caching Service
      hzConfig = ServiceConfig.getHazelcastConfig(conf.caching)
      defaultCache = EntityCacheMapper.getDefaultCache[F](hzConfig)
      entityCacheMap = EntityCacheMapper.getEntityCacheMap[F](hzConfig)
      responseCachingService = ResponseCachingService[F](entityCacheMap, defaultCache)
      authTokenCachingService: CachingServiceAlgebra[F] = CachingService[F](
        hzConfig.copy(authTokenTTL = CachingMap.AuthenticationAuthorisationToken.ttl),
        CachingMap.AuthenticationAuthorisationToken.name
      )

      // Message Orchestrator
      orchestrator = CollectionPointMessageService[F](
        responseCachingService,
        authTokenCachingService
      )

      // RabbitMQ Consumer and Router
      aMQPChannel <- rmqClient.createConnectionChannel
      rmqHandlers = new RMQMessageHandler[F](orchestrator)
      rmqRouter = RMQMessageRouter.getMessageHandlers[F](rmqHandlers)

      // Endpoint Handler
      endpointHandler = new CollectionPointEndpointHandler[F](orchestrator)
      endpointRoutes = new CollectionpointResource().routes(endpointHandler)

      // Health checkers
      checkers = NonEmptyList.of[HealthChecker[F]](SelfHealthCheck[F])
      healthCheckers = HealthCheckService(checkers)
      healthRoutes = new HealthcheckResource().routes(
        new HealthcheckAPIHandler[F](healthCheckers)
      )

      // Routes and HTTP App
      allRoutes = (healthRoutes <+> endpointRoutes).orNotFound
      httpApp = Logger.httpApp(logHeaders = true, logBody = true)(allRoutes)

      // Build server
      httpPort = Port.fromInt(conf.http.port.value)
      httpHost = Ipv4Address.fromString(conf.http.host.value)
      server <- EmberServerBuilder.default
        .withPort(httpPort.getOrElse(port"8300"))
        .withHost(httpHost.getOrElse(ipv4"0.0.0.0"))
        .withHttpApp(httpApp)
        .build
    } yield AppService(server, rmqRouter, rmqClient, aMQPChannel)
}
