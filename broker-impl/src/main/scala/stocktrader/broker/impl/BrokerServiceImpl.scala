package stocktrader.broker.impl

import akka.stream.Attributes
import akka.stream.scaladsl.Flow
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.NotFound
import stocktrader.OrderId
import stocktrader.broker.api.{BrokerService, OrderResult, OrderSummary, Quote}
import stocktrader.broker.impl.order.OrderRepository
import stocktrader.broker.impl.quote.QuoteService
import stocktrader.portfolio.api.{OrderPlaced, PortfolioService}

import scala.concurrent.{ExecutionContext, Future}

class BrokerServiceImpl(quoteService: QuoteService,
                        portfolioService: PortfolioService,
                        orderRepository: OrderRepository)(implicit ec: ExecutionContext)
  extends BrokerService {

  ??? // TODO: Register OrderEntity

  portfolioService.orderPlaced().subscribe.atLeastOnce(processPortfolioOrders())

  override def getQuote(symbol: String): ServiceCall[NotUsed, Quote] = { _ =>
    quoteService.getQuote(symbol)
  }

  override def getOrderSummary(orderId: OrderId): ServiceCall[NotUsed, OrderSummary] = { _ =>
    orderRepository.get(orderId).getSummary
      .map {
        case Some(summary) => summary
        case None => throw NotFound(s"Order not found: $orderId")
      }
  }

  override def orderResult(): Topic[OrderResult] = ???

  private def processPortfolioOrders(): Flow[OrderPlaced, Done, _] = {
    Flow[OrderPlaced]
      .log("orderPlaced")
      .addAttributes(Attributes.createLogLevels(
        onElement = Attributes.logLevelInfo,
        onFinish = Attributes.logLevelInfo,
        onFailure = Attributes.logLevelError
      ))
      // Note that order processing is asynchronous, so the parallelism parameter only limits how many
      // orders place at once before I get acknowledgement that they have been placed (which should be
      // essentially instant. It is not the maximum number of orders I can process concurrently.
      .mapAsync(10)(processOrder)
  }

  private def processOrder(order: OrderPlaced): Future[Done] = {
    orderRepository.get(order.orderId).placeOrder(order.portfolioId, order.orderDetails)
  }

}
