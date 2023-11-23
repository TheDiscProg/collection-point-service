package simex.server.entities

import enumeratum.{Enum, EnumEntry}

sealed trait ServiceError extends EnumEntry {
  val message: String
}

case object ServiceError extends Enum[ServiceError] {

  case class GeneralServiceError(message: String) extends ServiceError

  override def values: IndexedSeq[ServiceError] = findValues
}
