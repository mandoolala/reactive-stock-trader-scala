package stocktrader.portfolio.impl

import akka.NotUsed
import akka.persistence.query.Offset
import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, EventStreamElement, PersistentEntityRef, PersistentEntityRegistry}
import org.slf4j.{Logger, LoggerFactory}
import stocktrader.PortfolioId
import stocktrader.broker.api.BrokerService
import stocktrader.portfolio.api.{OpenPortfolioDetails, OrderPlaced}

import scala.concurrent.{ExecutionContext, Future}

class PortfolioRepositoryImpl(brokerService: BrokerService,
                              persistentEntities: PersistentEntityRegistry)(implicit ec: ExecutionContext)
  extends PortfolioRepository {

  private final val log: Logger = LoggerFactory.getLogger(classOf[PortfolioRepositoryImpl])

  /**
    * Initialize a new portfolio. We first generate a new ID for it and send it a setup message. In the very unlikely
    * circumstance that the ID is already in use we'll get an exception when we send the initialize command, we should
    * retry with a new UUID.
    *
    * @param request
    * @return The PortfolioModel ID assigned.
    */

  override def open(request: OpenPortfolioDetails): Future[PortfolioId] = {
    val portfolioId = PortfolioId.newId
    persistentEntities.refFor[PortfolioEntity](portfolioId)
      .ask(PortfolioCommand.Open(request.name))
      .map(_ => portfolioId)
  }

  override def get(portfolioId: PortfolioId): PortfolioModel = {
    new PortfolioModel(persistentEntities, portfolioId)
  }

  override def getRef(portfolioId: PortfolioId): PersistentEntityRef[PortfolioCommand] = {
    persistentEntities.refFor[PortfolioEntity](portfolioId)
  }

  override def ordersStream(tag: AggregateEventTag[PortfolioEvent], offset: Offset): Source[(OrderPlaced, Offset), NotUsed] = {
    persistentEntities.eventStream(tag, offset)
      .filter(e => e.event match {
        case _: PortfolioEvent.OrderPlaced => true
        case _ => false
      })
      .mapAsync(1) {
        case EventStreamElement(_, orderPlaced: PortfolioEvent.OrderPlaced, offset) =>
          log.info(s"Publishing order ${orderPlaced.orderId}")
          Future.successful((orderPlaced.asDomainEvent(), offset))
      }
  }

}
