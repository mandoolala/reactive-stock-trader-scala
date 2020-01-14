package stocktrader.broker.impl.trade

import stocktrader.broker.api.OrderResult
import stocktrader.portfolio.api.order.Order

import scala.concurrent.Future

trait TradeService {

  def placeOrder(order: Order): Future[OrderResult]

}
