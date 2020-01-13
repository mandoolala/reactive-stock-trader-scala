package stocktrader.portfolio.api

import play.api.libs.json.{Format, Json}
import stocktrader.PortfolioId

case class PortfolioSummary(portfolioId: PortfolioId, name: String)

object PortfolioSummary {
  implicit val format: Format[PortfolioSummary] = Json.format[PortfolioSummary]
}
