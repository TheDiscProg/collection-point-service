package simex.collectionpoint.domain.rabbitmq

import shareprice.rabbitmq.SharepriceQueue
import simex.rabbitmq.consumer.SimexMessageHandler

object RMQMessageRouter {

  def getMessageHandlers[F[_]](router: RMQMessageHandler[F]): Vector[SimexMessageHandler[F]] =
    Vector(
      SimexMessageHandler(
        SharepriceQueue.SERVICE_COLLECTION_POINT_QUEUE,
        router.handleReceivedMessage
      )
    )
}
