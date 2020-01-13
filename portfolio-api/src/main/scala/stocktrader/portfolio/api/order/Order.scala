package stocktrader.portfolio.api.order

import play.api.libs.json.{Format, Json}
import stocktrader.{OrderId, PortfolioId}

case class Order(orderId: OrderId, portfolioId: PortfolioId, details: OrderDetails)

object Order {
  implicit val format: Format[Order] = Json.format[Order]
}