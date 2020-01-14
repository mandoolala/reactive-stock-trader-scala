package stocktrader.portfolio.impl

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import julienrf.json.derived
import play.api.libs.json._
import stocktrader.broker.api.{OrderResult, Trade}
import stocktrader.portfolio.api.order.OrderDetails
import stocktrader.{OrderId, TransferId}

sealed trait PortfolioCommand

object PortfolioCommand {

  implicit val format: Format[PortfolioCommand] = derived.flat.oformat((__ \ "type").format[String])

  case object Liquidate extends PortfolioCommand with ReplyType[Done]
  case object GetState extends PortfolioCommand with ReplyType[PortfolioState.Open]

  case class Open(name: String) extends PortfolioCommand with ReplyType[Done]
  case class PlaceOrder(orderId: OrderId, orderDetails: OrderDetails) extends PortfolioCommand with ReplyType[Done]
  case class CompleteTrade(orderId: OrderId, trade: Trade) extends PortfolioCommand with ReplyType[Done]

  case class ReceiveFunds(amount: BigDecimal) extends PortfolioCommand with ReplyType[Done]
  case class SendFunds(amount: BigDecimal) extends PortfolioCommand with ReplyType[Done]
  case class AcceptRefund(amount: BigDecimal, transferId: TransferId) extends PortfolioCommand with ReplyType[Done]

  case class AcknowledgeOrderFailure(orderFailed: OrderResult.Failed) extends PortfolioCommand with ReplyType[Done]

  case object ClosePortfolio extends PortfolioCommand with ReplyType[Done]

}
