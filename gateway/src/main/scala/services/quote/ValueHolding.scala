package services.quote

import play.api.libs.json.Json

case class ValuedHolding(symbol: String, shareCount: Int, marketValue: Option[BigDecimal])

object ValuedHolding {
  implicit val format = Json.format[ValuedHolding]
}
