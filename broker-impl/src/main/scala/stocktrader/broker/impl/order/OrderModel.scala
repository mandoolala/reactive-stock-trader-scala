package stocktrader.broker.impl.order

import akka.Done
import stocktrader.PortfolioId
import stocktrader.broker.api.{OrderStatus, OrderSummary}
import stocktrader.portfolio.api.order.OrderDetails

import scala.concurrent.Future

trait OrderModel {

  def placeOrder(portfolioId: PortfolioId, orderDetails: OrderDetails): Future[Done]
  def getStatus: Option[OrderStatus]
  def getSummary: Future[Option[OrderSummary]]

}
