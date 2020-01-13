package stocktrader.portfolio.api

import play.api.libs.json.{Format, Json}
import stocktrader.{OrderId, PortfolioId}

case class PortfolioView(portfolioId: PortfolioId,
                         name: String,
                         funds: BigDecimal,
                         holdings: Seq[Holding],
                         completedOrders: Seq[OrderId])

object PortfolioView {
  implicit val format: Format[PortfolioView] = Json.format
}