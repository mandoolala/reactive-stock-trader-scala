package stocktrader.broker.impl.order

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.playjson.JsonSerializer
import play.api.libs.json.Json
import stocktrader.PortfolioId
import stocktrader.broker.api.{OrderResult, OrderStatus, OrderSummary}
import stocktrader.portfolio.api.order.{Order, OrderDetails}

sealed trait OrderCommand

object OrderCommand {

  case class PlaceOrder(portfolioId: PortfolioId, orderDetails: OrderDetails) extends OrderCommand with ReplyType[Order]

  object PlaceOrder {
    implicit val format = Json.format[PlaceOrder]
  }

  case class CompleteOrder(orderResult: OrderResult) extends OrderCommand with ReplyType[Done]

  object CompleteOrder {
    implicit val format = Json.format[CompleteOrder]
  }

  case object GetStatus extends OrderCommand with ReplyType[Option[OrderStatus]] {
    implicit val format = JsonSerializer.emptySingletonFormat(GetStatus)
  }

  case object GetSummary extends OrderCommand with ReplyType[Option[OrderSummary]] {
    implicit val format = JsonSerializer.emptySingletonFormat(GetSummary)
  }

}