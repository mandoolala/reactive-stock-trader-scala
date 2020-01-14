package stocktrader.broker.impl

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import stocktrader.broker.impl.order.{OrderCommand, OrderEvent, OrderState}

object BrokerSerializerRegistry extends JsonSerializerRegistry {
  override def serializers = List(
    // State
    JsonSerializer[OrderState.Pending],
    JsonSerializer[OrderState.Fulfilled],
    JsonSerializer[OrderState.Failed],
    // Commands
    JsonSerializer[OrderCommand.PlaceOrder],
    JsonSerializer[OrderCommand.CompleteOrder],
    JsonSerializer[OrderCommand.GetStatus.type],
    JsonSerializer[OrderCommand.GetSummary.type],
    // Events
    JsonSerializer[OrderEvent.OrderReceived],
    JsonSerializer[OrderEvent.OrderFulfilled],
    JsonSerializer[OrderEvent.OrderFailed],
  )
}
