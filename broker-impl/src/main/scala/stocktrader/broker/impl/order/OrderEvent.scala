package stocktrader.broker.impl.order

import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, AggregateEventTagger}
import play.api.libs.json.Json

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
  object OrderReceived {
    implicit val format = Json.format[OrderReceived]
  }

  case class OrderFulfilled(order: Order, trade: Trade) extends OrderEvent
  object OrderFulfilled {
    implicit val format = Json.format[OrderFulfilled]
  }

  case class OrderFailed(order: Order) extends OrderEvent
  object OrderFailed {
    implicit val format = Json.format[OrderFailed]
  }

}
