package services.quote

import stocktrader.broker.api.BrokerService
import stocktrader.portfolio.api.Holding

import scala.concurrent.{ExecutionContext, Future}

class QuoteServiceImpl(brokerService: BrokerService)(implicit ec: ExecutionContext) extends QuoteService {

  override def priceHoldings(holdings: Seq[Holding]): Future[Seq[ValuedHolding]] = {
    val requests = holdings.map { valuedHolding =>
      brokerService
        .getQuote(valuedHolding.symbol).invoke
        .map(quote => quote.sharePrice)
        .map(sharePrice => sharePrice * valuedHolding.shareCount)
        .map(price => ValuedHolding(valuedHolding.symbol, valuedHolding.shareCount, Some(price)))
        .recover {
          case _: RuntimeException => ValuedHolding(valuedHolding.symbol, valuedHolding.shareCount, None)
        }
    }
    Future.sequence(requests)
  }

}
