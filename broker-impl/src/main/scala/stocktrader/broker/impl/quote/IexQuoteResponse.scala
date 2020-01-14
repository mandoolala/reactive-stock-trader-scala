package stocktrader.broker.impl.quote

import play.api.libs.json.Json

case class IexQuoteResponse(symbol: String, latestPrice: BigDecimal)

object IexQuoteResponse {
  implicit val format = Json.format[IexQuoteResponse]
}
