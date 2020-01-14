package stocktrader.broker.impl.order

import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, AggregateEventTagger}
import stocktrader.broker.api.Trade
import stocktrader.portfolio.api.order.Order

sealed trait OrderEvent extends AggregateEvent[OrderEvent] {

  def order: Order
  override def aggregateTag: AggregateEventTagger[OrderEvent] = OrderEvent.Tag

}

object OrderEvent {

  val NumShards = 20
  val Tag = AggregateEventTag.sharded[OrderEvent](NumShards)

  case class OrderReceived(order: Order) extends OrderEvent
  case class OrderFulfilled(order: Order, trade: Trade) extends OrderEvent
  case class OrderFailed(order: Order) extends OrderEvent

}
