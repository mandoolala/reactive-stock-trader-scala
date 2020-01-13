package stocktrader.portfolio.api

import play.api.libs.json.{Format, Json}
import stocktrader.portfolio.api.order.OrderDetails
import stocktrader.{OrderId, PortfolioId}

case class OrderPlaced(portfolioId: PortfolioId, orderId: OrderId, orderDetails: OrderDetails)

object OrderPlaced {
  implicit val format: Format[OrderPlaced] = Json.format[OrderPlaced]
}

