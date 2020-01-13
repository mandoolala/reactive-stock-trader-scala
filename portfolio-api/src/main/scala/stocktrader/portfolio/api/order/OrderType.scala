package stocktrader.portfolio.api.order

import julienrf.json.derived
import play.api.libs.json._

sealed trait OrderType

object OrderType {

  case object Market extends OrderType
  case class Limit(limitPrice: BigDecimal) extends OrderType

  implicit val format: Format[OrderType] = derived.flat.oformat((__ \ "type").format[String])

}
