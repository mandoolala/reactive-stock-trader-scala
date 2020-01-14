package stocktrader.portfolio.impl

import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, AggregateEventTagger}
import julienrf.json.derived
import play.api.libs.json._
import stocktrader.portfolio.api.order.OrderDetails
import stocktrader.{OrderId, PortfolioId, TransferId}

sealed trait PortfolioEvent extends AggregateEvent[PortfolioEvent] {

  def portfolioId: PortfolioId
  override def aggregateTag: AggregateEventTagger[PortfolioEvent] = PortfolioEvent.Tag

}

object PortfolioEvent {

  implicit val format: Format[PortfolioEvent] = derived.flat.oformat((__ \ "type").format[String])

  val NumShards = 20
  val Tag = AggregateEventTag.sharded[PortfolioEvent](NumShards)

  case class Opened(portfolioId: PortfolioId, name: String) extends PortfolioEvent
  case class LiquidationStarted(portfolioId: PortfolioId) extends PortfolioEvent
  case class Closed(portfolioId: PortfolioId) extends PortfolioEvent

  case class SharesCredited(portfolioId: PortfolioId, symbol: String, shares: Int) extends PortfolioEvent
  case class SharesDebited(portfolioId: PortfolioId, symbol: String, shares: Int) extends PortfolioEvent

  case class FundsDebited(portfolioId: PortfolioId, amount: BigDecimal) extends PortfolioEvent
  case class FundsCredited(portfolioId: PortfolioId, amount: BigDecimal) extends PortfolioEvent

  case class RefundAccepted(portfolioId: PortfolioId, transferId: TransferId, amount: BigDecimal) extends PortfolioEvent

  case class OrderPlaced(orderId: OrderId, portfolioId: PortfolioId, orderDetails: OrderDetails) extends PortfolioEvent {
    def asDomainEvent(): OrderPlaced = OrderPlaced(orderId, portfolioId, orderDetails)
  }
  case class OrderFulfilled(portfolioId: PortfolioId, orderId: OrderId) extends PortfolioEvent
  case class OrderFailed(portfolioId: PortfolioId, orderId: OrderId) extends PortfolioEvent

}
