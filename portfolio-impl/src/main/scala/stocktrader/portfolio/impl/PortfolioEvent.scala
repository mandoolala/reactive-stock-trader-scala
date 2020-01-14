package stocktrader.portfolio.impl

import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, AggregateEventTagger}

import play.api.libs.json.Json

import stocktrader.portfolio.api
import stocktrader.portfolio.api.order.OrderDetails
import stocktrader.{OrderId, PortfolioId, TransferId}

sealed trait PortfolioEvent extends AggregateEvent[PortfolioEvent] {

  def portfolioId: PortfolioId
  override def aggregateTag: AggregateEventTagger[PortfolioEvent] = PortfolioEvent.Tag

}

object PortfolioEvent {

  val NumShards = 20
  val Tag = AggregateEventTag.sharded[PortfolioEvent](NumShards)

  case class Opened(portfolioId: PortfolioId, name: String) extends PortfolioEvent
  object Opened {
    implicit val format = Json.format[Opened]
  }

  case class LiquidationStarted(portfolioId: PortfolioId) extends PortfolioEvent
  object LiquidationStarted {
    implicit val format = Json.format[LiquidationStarted]
  }

  case class Closed(portfolioId: PortfolioId) extends PortfolioEvent
  object Closed {
    implicit val format = Json.format[Closed]
  }

  case class SharesCredited(portfolioId: PortfolioId, symbol: String, shares: Int) extends PortfolioEvent
  object SharesCredited {
    implicit val format = Json.format[SharesCredited]
  }

  case class SharesDebited(portfolioId: PortfolioId, symbol: String, shares: Int) extends PortfolioEvent
  object SharesDebited {
    implicit val format = Json.format[SharesDebited]
  }

  case class FundsDebited(portfolioId: PortfolioId, amount: BigDecimal) extends PortfolioEvent
  object FundsDebited {
    implicit val format = Json.format[FundsDebited]
  }

  case class FundsCredited(portfolioId: PortfolioId, amount: BigDecimal) extends PortfolioEvent
  object FundsCredited {
    implicit val format = Json.format[FundsCredited]
  }

  case class RefundAccepted(portfolioId: PortfolioId, transferId: TransferId, amount: BigDecimal) extends PortfolioEvent
  object RefundAccepted {
    implicit val format = Json.format[RefundAccepted]
  }

  case class OrderPlaced(orderId: OrderId, portfolioId: PortfolioId, orderDetails: OrderDetails) extends PortfolioEvent {
    def asDomainEvent(): api.OrderPlaced = api.OrderPlaced(orderId, portfolioId, orderDetails)
  }
  object OrderPlaced {
    implicit val format = Json.format[OrderPlaced]
  }

  case class OrderFulfilled(portfolioId: PortfolioId, orderId: OrderId) extends PortfolioEvent
  object OrderFulfilled {
    implicit val format = Json.format[OrderFulfilled]
  }

  case class OrderFailed(portfolioId: PortfolioId, orderId: OrderId) extends PortfolioEvent
  object OrderFailed {
    implicit val format = Json.format[OrderFailed]
  }

}

