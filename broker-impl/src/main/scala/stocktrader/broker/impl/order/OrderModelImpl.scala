package stocktrader.broker.impl.order

import akka.Done

import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRef

import org.slf4j.{Logger, LoggerFactory}

import stocktrader.{OrderId, PortfolioId}
import stocktrader.broker.api.{OrderResult, OrderStatus, OrderSummary}
import stocktrader.broker.impl.trade.TradeService
import stocktrader.portfolio.api.order.{Order, OrderDetails}

import scala.concurrent.{ExecutionContext, Future}

class OrderModelImpl(orderEntity: PersistentEntityRef[OrderCommand], tradeService: TradeService)(implicit ec: ExecutionContext)
  extends OrderModel {

  private final val log: Logger = LoggerFactory.getLogger(classOf[OrderModelImpl])

  override def placeOrder(portfolioId: PortfolioId, orderDetails: OrderDetails): Future[Done] = {
    val placeOrderF: Future[Order] = orderEntity.ask(OrderCommand.PlaceOrder(portfolioId, orderDetails))

    // This is the process that will progress our order through to completion.
    // if the service is interrupted before this is completed we will not reattempt. Consider the implications.
    placeOrderF.foreach { order =>
      tradeService.placeOrder(order)
        .recover {
          case ex: Throwable =>
            val orderId: OrderId = OrderId(orderEntity.entityId)
            log.info(s"Order $orderId failed, ${ex.toString}.", ex)
            OrderResult.Failed(order.portfolioId, orderId)
        }
        .foreach { orderResult =>
          log.info(s"Order ${orderEntity.entityId} completing.")
          orderEntity.ask(OrderCommand.CompleteOrder(orderResult))
        }
    }

    // Note that service call responds with Done after the PlaceOrder command is accepted,
    // it does not wait for the order to be fulfilled (which, in general, may require some time).
    placeOrderF.map(_ => Done)
  }

  override def getSummary: Future[Option[OrderSummary]] = ???

  override def getStatus: Option[OrderStatus] = ???

}
