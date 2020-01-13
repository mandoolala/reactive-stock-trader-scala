package stocktrader.portfolio.api

import play.api.libs.json.{Format, Json}

case class Holding(symbol: String, shareCount: Int)

object Holding {
  implicit val format: Format[Holding] = Json.format
}