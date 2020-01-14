package stocktrader.broker.impl.quote

import stocktrader.broker.api.Quote

import scala.concurrent.Future

trait QuoteService {

  def getQuote(symbol: String): Future[Quote]

}
