package stocktrader.portfolio.api

import play.api.libs.json.{Format, Json}

case class OpenPortfolioDetails(name: String)

object OpenPortfolioDetails {
  implicit val format: Format[OpenPortfolioDetails] = Json.format[OpenPortfolioDetails]
}