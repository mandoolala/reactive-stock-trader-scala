package models

import play.api.libs.json.Json
import services.quote.ValuedHolding
import stocktrader.PortfolioId

case class Portfolio(portfolioId: PortfolioId, name: String, funds: BigDecimal, holdings: Seq[ValuedHolding])

object Portfolio {
  implicit val format = Json.format[Portfolio]
}
