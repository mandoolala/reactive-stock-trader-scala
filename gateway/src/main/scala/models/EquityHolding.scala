package models

import play.api.libs.json.Json

case class EquityHolding(symbol: String, shares: Int, sharePrice: BigDecimal)

object EquityHolding {
  implicit val format = Json.format[EquityHolding]
}
