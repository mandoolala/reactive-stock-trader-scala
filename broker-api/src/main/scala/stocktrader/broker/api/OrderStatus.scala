package stocktrader.broker.api

import julienrf.json.derived
import play.api.libs.json._

sealed trait OrderStatus

object OrderStatus {

  case object Pending extends OrderStatus
  case class Fulfilled(price: BigDecimal) extends OrderStatus
  case object Failed extends OrderStatus

  implicit val format: Format[OrderStatus] = derived.flat.oformat((__ \ "type").format[String])

}
