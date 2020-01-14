package stocktrader.broker.impl.order

import akka.persistence.query.Offset
import akka.stream.scaladsl.Source

import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, EventStreamElement, PersistentEntityRegistry}

import org.slf4j.{Logger, LoggerFactory}

import stocktrader.OrderId
import stocktrader.broker.api.OrderResult
import stocktrader.broker.impl.trade.TradeService

import scala.concurrent.ExecutionContext


class OrderRepositoryImpl(persistentEntities: PersistentEntityRegistry, tradeService: TradeService)(implicit ec: ExecutionContext)
  extends OrderRepository {

  private final val log: Logger = LoggerFactory.getLogger(classOf[OrderRepositoryImpl])

  override def get(orderId: OrderId): OrderModel = createModel(orderId)

  override def orderResults(tag: AggregateEventTag[OrderEvent], offset: Offset): Source[(OrderResult, Offset), _] = {
    persistentEntities.eventStream(tag, offset)
      .filter(e => e.event match {
        case _: OrderEvent.OrderFulfilled => true
        case _: OrderEvent.OrderFailed => true
        case _ => false
      })
      .map {
        case EventStreamElement(_, fulfilled: OrderEvent.OrderFulfilled, offset) =>
          val order = fulfilled.order
          val trade = fulfilled.trade
          log.info(s"Order ${order.orderId} fulfilled.");
          val completedOrder = OrderResult.Fulfilled(order.portfolioId, order.orderId, trade)
          (completedOrder, offset)
        case EventStreamElement(_, failed: OrderEvent.OrderFailed, offset) =>
          (OrderResult.Failed(failed.order.portfolioId, failed.order.orderId), offset)
        case _ => throw new IllegalStateException()
      }
  }

  private def createModel(orderId: OrderId): OrderModel = {
    new OrderModelImpl(persistentEntities.refFor[OrderEntity](orderId), tradeService)
  }

}
