package stocktrader.wiretransfer.impl

import akka.NotUsed
import akka.persistence.query.Offset
import akka.stream.scaladsl.Source

import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, EventStreamElement}
import com.lightbend.lagom.scaladsl.pubsub.{PubSubRegistry, TopicId}

import stocktrader.wiretransfer.api.{TransactionSummary, Transfer, TransferRequest, WireTransferService}
import stocktrader.wiretransfer.impl.transfer.{TransferCommand, TransferEvent, TransferRepository}
import stocktrader.{PortfolioId, TransferId}

import scala.concurrent.{ExecutionContext, Future}

class WireTransferServiceImpl(transferRepository: TransferRepository,
                              db: CassandraSession,
                              pubSub: PubSubRegistry)(implicit val ex: ExecutionContext) extends WireTransferService {

  override def transferFunds(): ServiceCall[Transfer, TransferId] = { transfer =>
    val transferId = TransferId.newId
    transferRepository.get(transferId)
      .ask(TransferCommand.TransferFunds(transfer.sourceAccount, transfer.destinationAccount, transfer.funds))
      .map(_ => transferId)
  }

  override def transferRequest(): Topic[TransferRequest] = {
    TopicProducer.taggedStreamWithOffset(TransferEvent.Tag.allTags.toList) { (tag, offset) =>
      transferRequestSource(tag, offset)
    }
  }

  override def getAllTransactionsFor(portfolioId: PortfolioId): ServiceCall[NotUsed, Seq[TransactionSummary]] = { _ =>
    db.selectAll("SELECT transferId, status, dateTime, source, destination, amount FROM transfer_summary;")
      .map(rows => rows
        .map(row => TransactionSummary(
          id = row.getString("transferId"),
          status = row.getString("status"),
          dateTime = row.getString("dateTime"),
          source = row.getString("source"),
          destination = row.getString("destination"),
          amount = row.getString("amount")
        ))
        .filter(s => s.source == portfolioId || s.destination == portfolioId)
      )
  }

  override def transferStream(): ServiceCall[NotUsed, Source[String, NotUsed]] = { _ =>
    val topic = pubSub.refFor(TopicId("transfer"))
    Future.successful(topic.subscriber)
  }

  private def transferRequestSource(tag: AggregateEventTag[TransferEvent], offset: Offset): Source[(TransferRequest, Offset), _] = {
    transferRepository.eventStream(tag, offset)
      .collect {
        case EventStreamElement(_, event: TransferEvent.TransferInitiated, _) => (requestFunds(event), offset)
        case EventStreamElement(_, event: TransferEvent.FundsRetrieved, _) => (sendFunds(event), offset)
      }
  }

  private def requestFunds(event: TransferEvent.TransferInitiated): TransferRequest = {
    TransferRequest.WithdrawalRequest(
      transferId = event.transferId,
      account = event.transferDetails.source,
      amount = event.transferDetails.amount
    )
  }

  private def sendFunds(event: TransferEvent.FundsRetrieved): TransferRequest = {
    TransferRequest.DepositRequest(
      transferId = event.transferId,
      account = event.transferDetails.destination,
      amount = event.transferDetails.amount
    )
  }

}
