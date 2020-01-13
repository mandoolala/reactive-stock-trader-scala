package stocktrader.broker.api

import play.api.libs.json.{Format, Json}

case class Quote(symbol: String, sharePrice: BigDecimal)

object Quote {
  implicit val format: Format[Quote] = Json.format
}
