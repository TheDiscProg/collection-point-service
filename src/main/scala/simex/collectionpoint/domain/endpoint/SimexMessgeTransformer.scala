package simex.collectionpoint.domain.endpoint

import simex.guardrail.definitions.SimexMessage
import simex.messaging.Simex
import io.scalaland.chimney.dsl._
import scala.util.Try

object SimexMessgeTransformer {

  def transformRequest(request: SimexMessage): Option[Simex] =
    Try {
      request
        .into[Simex]
        .transform
    }.toOption

  def transformToResponse(message: Simex): SimexMessage =
    message
      .into[SimexMessage]
      .transform

}
