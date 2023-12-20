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
        .withFieldComputed(_.originator.messageTTL, r => r.originator.messageTtl)
        .transform
    }.toOption

  def transformToResponse(message: Simex): SimexMessage =
    message
      .into[SimexMessage]
      .withFieldComputed(_.originator.messageTtl, m => m.originator.messageTTL)
      .transform

}
