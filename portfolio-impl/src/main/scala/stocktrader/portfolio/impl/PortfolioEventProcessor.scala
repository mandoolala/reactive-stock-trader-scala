package stocktrader.portfolio.impl

import akka.Done
import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future, Promise}

class PortfolioEventProcessor(session: CassandraSession, readSide: CassandraReadSide)(implicit ec: ExecutionContext)
  extends ReadSideProcessor[PortfolioEvent] {

  private final val log: Logger = LoggerFactory.getLogger(classOf[PortfolioEventProcessor])

  private val writePortfoliosPromise = Promise[PreparedStatement]

  private def writePortfolios: Future[PreparedStatement] = writePortfoliosPromise.future

  override def aggregateTags: Set[AggregateEventTag[PortfolioEvent]] = PortfolioEvent.Tag.allTags

  override def buildHandler(): ReadSideProcessor.ReadSideHandler[PortfolioEvent] = {
    readSide.builder[PortfolioEvent]("portfolio_offset")
      .setGlobalPrepare(prepareCreateTables)
      .setPrepare(_ => prepareWritePortfolios())
      .setEventHandler[PortfolioEvent.Opened](e => processPortfolioChanged(e.event))
      .build()
  }

  private def prepareCreateTables(): Future[Done] = session.executeCreateTable(
    """CREATE TABLE IF NOT EXISTS portfolio_summary (
      |  portfolioId text,
      |  name text,
      |  PRIMARY KEY (portfolioId)
      |)
      |""".stripMargin)

  private def prepareWritePortfolios(): Future[Done] = {
    val f = session.prepare(
      """INSERT INTO portfolio_summary (portfolioId, name)
        |VALUES (?, ?)
        |""".stripMargin)
    writePortfoliosPromise.completeWith(f)
    f.map(_ => Done)
  }

  private def processPortfolioChanged(event: PortfolioEvent.Opened): Future[List[BoundStatement]] = {
    log.debug(s"Portfolio opened: ${event.portfolioId}")
    writePortfolios.map { ps =>
      val bindWritePortfolios = ps.bind()
      bindWritePortfolios.setString("portfolioId", event.portfolioId)
      bindWritePortfolios.setString("name", event.name)
      List(bindWritePortfolios)
    }
  }

}
