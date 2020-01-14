package stocktrader.portfolio.impl

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.playjson.JsonSerializer
import play.api.libs.json.Json
import stocktrader.broker.api.{OrderResult, Trade}
import stocktrader.portfolio.api.order.OrderDetails
import stocktrader.{OrderId, TransferId}

sealed trait PortfolioCommand

object PortfolioCommand {

  case object Liquidate extends PortfolioCommand with ReplyType[Done] {
    implicit val format = JsonSerializer.emptySingletonFormat(Liquidate)
  }

  case object GetState extends PortfolioCommand with ReplyType[PortfolioState.Open] {
    implicit val format = JsonSerializer.emptySingletonFormat(GetState)
  }

  case class Open(name: String) extends PortfolioCommand with ReplyType[Done]

  object Open {
    implicit val format = Json.format[Open]
  }

  case class PlaceOrder(orderId: OrderId, orderDetails: OrderDetails) extends PortfolioCommand with ReplyType[Done]

  object PlaceOrder {
    implicit val format = Json.format[PlaceOrder]
  }

  case class CompleteTrade(orderId: OrderId, trade: Trade) extends PortfolioCommand with ReplyType[Done]

  object CompleteTrade {
    implicit val format = Json.format[CompleteTrade]
  }

  case class ReceiveFunds(amount: BigDecimal) extends PortfolioCommand with ReplyType[Done]

  object ReceiveFunds {
    implicit val format = Json.format[ReceiveFunds]
  }

  case class SendFunds(amount: BigDecimal) extends PortfolioCommand with ReplyType[Done]

  object SendFunds {
    implicit val format = Json.format[SendFunds]
  }

  case class AcceptRefund(amount: BigDecimal, transferId: TransferId) extends PortfolioCommand with ReplyType[Done]

  object AcceptRefund {
    implicit val format = Json.format[AcceptRefund]
  }

  case class AcknowledgeOrderFailure(orderFailed: OrderResult.Failed) extends PortfolioCommand with ReplyType[Done]

  object AcknowledgeOrderFailure {
    implicit  val orderResultFailedFormat = Json.format[OrderResult.Failed]
    implicit val format = Json.format[AcknowledgeOrderFailure]
  }

  case object ClosePortfolio extends PortfolioCommand with ReplyType[Done] {
    implicit val format = JsonSerializer.emptySingletonFormat(ClosePortfolio)
  }

}
