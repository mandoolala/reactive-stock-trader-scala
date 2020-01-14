package stocktrader.broker.impl.order

import akka.persistence.query.Offset
import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag
import stocktrader.OrderId
import stocktrader.broker.api.OrderResult

trait OrderRepository {

  def get(orderId: OrderId): OrderModel
  def orderResults(tag: AggregateEventTag[OrderEvent], offset: Offset): Source[(OrderResult, Offset), _]

}
