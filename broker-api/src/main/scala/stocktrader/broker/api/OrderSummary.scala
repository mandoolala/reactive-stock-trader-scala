package stocktrader.broker.api

import play.api.libs.json.Json
import stocktrader.TradeType._
import stocktrader.{OrderId, PortfolioId}

case class OrderSummary(orderId: OrderId,
                        portfolioId: PortfolioId,
                        tradeType: TradeType,
                        symbol: String,
                        shares: Int,
                        status: OrderStatus)

object OrderSummary {
  implicit val format = Json.format[OrderSummary]
}
