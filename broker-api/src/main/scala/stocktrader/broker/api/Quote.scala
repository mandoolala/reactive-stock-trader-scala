package stocktrader.broker.api

import play.api.libs.json.{Json, Format}

case class Quote(symbol: String, sharePrice: BigDecimal)

object Quote {
implicit val format: Format[Quote] = Json.format
}
