package stocktrader.portfolio.impl

import akka.NotUsed
import akka.persistence.query.Offset
import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, PersistentEntityRef}
import stocktrader.PortfolioId
import stocktrader.portfolio.api.{OpenPortfolioDetails, OrderPlaced}


import scala.concurrent.Future

trait PortfolioRepository {

  def open(request: OpenPortfolioDetails): Future[PortfolioId]
  def get(portfolioId: PortfolioId): PortfolioModel
  def getRef(portfolioId: PortfolioId): PersistentEntityRef[PortfolioCommand]
  def ordersStream(tag: AggregateEventTag[PortfolioEvent], offset: Offset): Source[(OrderPlaced, Offset), NotUsed]

}
