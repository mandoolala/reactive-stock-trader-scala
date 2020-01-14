package stocktrader.broker.impl.trade

import org.slf4j.{Logger, LoggerFactory}
import stocktrader.broker.api.{OrderResult, Trade}
import stocktrader.broker.impl.quote.QuoteService
import stocktrader.portfolio.api.order.{Order, OrderDetails, OrderType}

import scala.concurrent.{ExecutionContext, Future}

class TradeServiceImpl(quoteService: QuoteService)(implicit ec: ExecutionContext) extends TradeService {

  private final val log: Logger = LoggerFactory.getLogger(classOf[TradeServiceImpl])

  override def placeOrder(order: Order): Future[OrderResult] = {
    log.info(s"Order placed: $order")
    order.details.orderType match {
      case _: OrderType.Market.type =>
        completeMarketOrder(order)
      case _: OrderType.Limit =>
        log.info(s"Unhanded order placed: ${order.details.orderType}")
        Future.failed(new UnsupportedOperationException) // TODO
    }
  }

  private def completeMarketOrder(order: Order): Future[OrderResult] = {
    priceOrder(order)
      .map { price =>
        val details: OrderDetails = order.details
        val trade = Trade(
          symbol = details.symbol,
          shares = details.shares,
          tradeType = details.tradeType,
          sharePrice = price
        )
        OrderResult.Fulfilled(order.portfolioId, order.orderId, trade)
      }
  }

  private def priceOrder(order: Order): Future[BigDecimal] = {
    quoteService.getQuote(order.details.symbol)
      .map { quote =>
        quote.sharePrice * order.details.shares
      }
  }

}
