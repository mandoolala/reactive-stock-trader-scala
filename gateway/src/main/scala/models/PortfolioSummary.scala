package models

import play.api.libs.json.Json
import stocktrader.PortfolioId

case class PortfolioSummary(portfolioId: PortfolioId,
                            name: String,
                            funds: BigDecimal,
                            equities: Seq[EquityHolding],
                            completedOrders: Seq[CompletedOrder])

object PortfolioSummary {
  implicit val format = Json.format[PortfolioSummary]
}
