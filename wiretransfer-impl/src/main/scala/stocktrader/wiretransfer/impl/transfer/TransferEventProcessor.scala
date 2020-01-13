package stocktrader.wiretransfer.impl.transfer

import java.sql.Timestamp

import akka.Done
import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import stocktrader.wiretransfer.api.Account

import scala.concurrent.{ExecutionContext, Future, Promise}

class TransferEventProcessor(session: CassandraSession, readSide: CassandraReadSide)(implicit ec: ExecutionContext)
  extends ReadSideProcessor[TransferEvent] {

  private val writeTransfersPromise = Promise[PreparedStatement]

  private def writeTransfers: Future[PreparedStatement] = writeTransfersPromise.future

  override def aggregateTags: Set[AggregateEventTag[TransferEvent]] = TransferEvent.Tag.allTags

  override def buildHandler(): ReadSideProcessor.ReadSideHandler[TransferEvent] = {
    readSide.builder[TransferEvent]("transfer_offset")
      .setGlobalPrepare(() => prepareCreateTables())
      .setPrepare(_ => prepareWriteTransfers())
      .setEventHandler[TransferEvent.TransferInitiated](e => processTransferInitiated(e.event))
      .setEventHandler[TransferEvent.FundsRetrieved](e => processFundsRetrieved(e.event))
      .setEventHandler[TransferEvent.CouldNotSecureFunds](e => processCouldNotSecureFunds(e.event))
      .setEventHandler[TransferEvent.DeliveryConfirmed](e => processDeliveryConfirmed(e.event))
      .setEventHandler[TransferEvent.DeliveryFailed](e => processDeliveryFailed(e.event))
      .setEventHandler[TransferEvent.RefundDelivered](e => processRefundDelivered(e.event))
      .build()
  }

  //TODO: create table if not exists - replace string
  //transfer_summary (transferId, status, dateTime, source, destination, amount)
  private def prepareCreateTables(): Future[Done] = session.executeCreateTable(
    "string".stripMargin
  )

  //TODO: Insert into transfer_summary - replace string
  //transfer_summary (transferId, status, dateTime, source, destination, amount)
  private def prepareWriteTransfers(): Future[Done] = {
    val f = session.prepare("string".stripMargin)
    writeTransfersPromise.completeWith(f)
    f.map(_ => Done)
  }

  private def processTransferInitiated(event: TransferEvent.TransferInitiated): Future[List[BoundStatement]] = {
    processTransferEvent("Transfer Initiated", event)
  }

  private def processFundsRetrieved(event: TransferEvent.FundsRetrieved): Future[List[BoundStatement]] = {
    processTransferEvent("Funds Retrieved", event)
  }

  private def processCouldNotSecureFunds(event: TransferEvent.CouldNotSecureFunds): Future[List[BoundStatement]] = {
    processTransferEvent("Could Not Secure Funds", event)
  }

  private def processDeliveryConfirmed(event: TransferEvent.DeliveryConfirmed): Future[List[BoundStatement]] = {
    processTransferEvent("Delivery Confirmed", event)
  }

  private def processDeliveryFailed(event: TransferEvent.DeliveryFailed): Future[List[BoundStatement]] = {
    processTransferEvent("Delivery Failed", event)
  }

  private def processRefundDelivered(event: TransferEvent.RefundDelivered): Future[List[BoundStatement]] = {
    processTransferEvent("Refund Delivered", event)
  }

  private def processTransferEvent(status: String, event: TransferEvent): Future[List[BoundStatement]] = {
    val timestamp = new Timestamp(System.currentTimeMillis)
    val source = event.transferDetails.source match {
      case portfolio: Account.Portfolio => portfolio.portfolioId
      case _ => "Savings"
    }
    val destination = event.transferDetails.destination match {
      case portfolio: Account.Portfolio => portfolio.portfolioId
      case _ => "Savings"
    }
    val amount = event.transferDetails.amount.toString()

    writeTransfers.map { ps =>
      val bindWriteTransfers = ps.bind()
      bindWriteTransfers.setString("transferId", event.transferId)
      bindWriteTransfers.setString("status", status)
      bindWriteTransfers.setString("dateTime", timestamp.toString)
      bindWriteTransfers.setString("source", source)
      bindWriteTransfers.setString("destination", destination)
      bindWriteTransfers.setString("amount", amount)
      List(bindWriteTransfers)
    }
  }

}
