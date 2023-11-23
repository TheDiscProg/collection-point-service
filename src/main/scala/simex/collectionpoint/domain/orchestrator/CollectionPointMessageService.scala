package simex.collectionpoint.domain.orchestrator
import cats.Monad
import org.typelevel.log4cats.Logger
import simex.collectionpoint.domain.caching.ResponseCachingServiceAlgebra
import simex.messaging.{Security, Simex}
import cats.syntax.all._
import shareprice.entity.EntitySecurity
import simex.caching.CachingServiceAlgebra

class CollectionPointMessageService[F[_]: Monad: Logger](
    responseCachingService: ResponseCachingServiceAlgebra[F],
    authTokenCachingService: CachingServiceAlgebra[F]
) extends CollectionPointMessageHandlerAlgebra[F] {

  override def handleSimexMessage(message: Simex): F[Unit] =
    for {
      _ <- Logger[F].info(s"CollectionPointHandler: Received message: ${message.endpoint.entity}")
      key = s"${message.originator.clientId}-${message.originator.requestId}"
      _ <- responseCachingService.save(key, message)
    } yield ()

  override def getResponse(request: Simex): F[Option[Simex]] =
    for {
      savedMessage <- responseCachingService.getResponse(request)
      response <- savedMessage.fold(
        (None: Option[Simex]).pure[F]
      )(msg => checkSecurity(request, msg))
    } yield response

  private def checkSecurity(request: Simex, response: Simex): F[Option[Simex]] = {
    val security: EntitySecurity = EntitySecurity.fromEntity(response.endpoint.entity)
    security.security match {
      case Security.BASIC => checkClientId(request, response).pure[F]
      case Security.AUTHORIZED => checkAuthorizationSecurity(request, response)
      case Security.ORIGINAL_TOKEN =>
        for {
          check <- checkAuthorizationSecurity(request, response)
          checkResponse =
            if (check.isDefined && checkOriginalTokenSecurity(request, response))
              check
            else
              None
        } yield checkResponse
      case Security.FORBIDDEN => (None: Option[Simex]).pure[F]
    }
  }

  private def checkAuthorizationSecurity(request: Simex, response: Simex): F[Option[Simex]] =
    for {
      user <- authTokenCachingService.getMessage(request.getAuthorization)
      result = user.fold(None: Option[Simex]) { client =>
        checkClientId(request, client).map(_ => response)
      }
    } yield result

  private def checkClientId(request: Simex, response: Simex): Option[Simex] =
    if (response.originator.clientId == request.client.clientId) {
      Some(response)
    } else {
      None
    }

  private def checkOriginalTokenSecurity(request: Simex, response: Simex): Boolean =
    request.originator.originalToken == response.originator.originalToken
}

object CollectionPointMessageService {
  def apply[F[_]: Monad: Logger](
      responseCachingService: ResponseCachingServiceAlgebra[F],
      authTokenCachingService: CachingServiceAlgebra[F]
  ) =
    new CollectionPointMessageService[F](responseCachingService, authTokenCachingService)
}
