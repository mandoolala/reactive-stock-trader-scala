package stocktrader.broker.impl.order

import play.api.libs.json.Json
import stocktrader.PortfolioId
import stocktrader.broker.api.OrderStatus
import stocktrader.portfolio.api.order.OrderDetails

sealed trait OrderState {

  def orderDetails: OrderDetails
  def status: OrderStatus
  def portfolioId: PortfolioId

}

object OrderState {

  case class Pending(portfolioId: PortfolioId, orderDetails: OrderDetails) extends OrderState {
    override def status: OrderStatus = OrderStatus.Pending
  }
  object Pending {
    implicit val format = Json.format[Pending]
  }

  case class Fulfilled(portfolioId: PortfolioId, orderDetails: OrderDetails, price: BigDecimal) extends OrderState {
    override def status: OrderStatus = OrderStatus.Fulfilled(price)
  }
  object Fulfilled {
    implicit val format = Json.format[Fulfilled]
  }

  case class Failed(portfolioId: PortfolioId, orderDetails: OrderDetails) extends OrderState {
    override def status: OrderStatus = OrderStatus.Failed
  }
  object Failed {
    implicit val format = Json.format[Failed]
  }

}
