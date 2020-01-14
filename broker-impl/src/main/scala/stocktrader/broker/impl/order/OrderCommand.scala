package stocktrader.broker.impl.order

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import stocktrader.PortfolioId
import stocktrader.broker.api.{OrderResult, OrderStatus, OrderSummary}
import stocktrader.portfolio.api.order.{Order, OrderDetails}

sealed trait OrderCommand

object OrderCommand {

  case class PlaceOrder(portfolioId: PortfolioId, orderDetails: OrderDetails) extends OrderCommand with ReplyType[Order]
  case class CompleteOrder(orderResult: OrderResult) extends OrderCommand with ReplyType[Done]
  case object GetStatus extends OrderCommand with ReplyType[Option[OrderStatus]]
  case object GetSummary extends OrderCommand with ReplyType[Option[OrderSummary]]

}
