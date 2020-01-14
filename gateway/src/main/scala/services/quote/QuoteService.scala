package services.quote

import stocktrader.portfolio.api.Holding

import scala.concurrent.Future

trait QuoteService {

  def priceHoldings(holdings: Seq[Holding]): Future[Seq[ValuedHolding]]

}
