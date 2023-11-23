package simex.collectionpoint.domain.endpoint

import cats.effect.MonadCancel
import org.typelevel.log4cats.Logger
import simex.collectionpoint.domain.orchestrator.CollectionPointMessageHandlerAlgebra
import simex.guardrail.collectionpoint.{CollectionpointHandler, CollectionpointResource}
import simex.guardrail.definitions.SimexMessage
import cats.syntax.all._
import simex.guardrail.collectionpoint.CollectionpointResource.ColllectPointRequestResponse
import simex.guardrail.collectionpoint.CollectionpointResource.ColllectPointRequestResponse.{
  BadRequest,
  NoContent,
  Ok
}
import simex.messaging.Simex
import simex.server.entities.EndpointServiceResponse

class CollectionPointEndpointHandler[F[_]: Logger: MonadCancel[*[_], Throwable]](
    orc: CollectionPointMessageHandlerAlgebra[F]
) extends CollectionpointHandler[F] {

  override def colllectPointRequest(
      respond: CollectionpointResource.ColllectPointRequestResponse.type
  )(body: SimexMessage): F[CollectionpointResource.ColllectPointRequestResponse] =
    for {
      _ <- Logger[F].info(s"Collection Point Request: $body")
      requestOption = SimexMessgeTransformer.transformRequest(body)
      response <- requestOption match {
        case Some(request) =>
          if (Simex.checkEndPointValidity(request))
            handleRequest(request)
          else
            EndpointServiceResponse[ColllectPointRequestResponse](BadRequest).pure[F]
        case None =>
          EndpointServiceResponse[ColllectPointRequestResponse](BadRequest).pure[F]
      }
    } yield response.returnType

  private def handleRequest(
      request: Simex
  ): F[EndpointServiceResponse[ColllectPointRequestResponse]] =
    for {
      messageOption <- orc.getResponse(request)
      response <- messageOption.fold(
        EndpointServiceResponse[ColllectPointRequestResponse](NoContent).pure[F]
      )(msg =>
        EndpointServiceResponse[ColllectPointRequestResponse](
          Ok(SimexMessgeTransformer.transformToResponse(msg))
        ).pure[F]
      )
    } yield response

}
