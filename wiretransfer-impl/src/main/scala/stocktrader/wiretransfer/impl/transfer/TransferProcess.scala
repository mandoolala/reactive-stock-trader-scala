package stocktrader.wiretransfer.impl.transfer

import akka.stream.Attributes
import akka.stream.scaladsl.Flow
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, EventStreamElement, ReadSideProcessor}
import stocktrader.portfolio.api.{Deposit, PortfolioService, Refund, Withdrawal}
import stocktrader.wiretransfer.api.Account
import stocktrader.wiretransfer.impl.transfer.TransferEvent._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class TransferProcess(portfolioService: PortfolioService, transferRepository: TransferRepository)(implicit ec: ExecutionContext)
  extends ReadSideProcessor[TransferEvent] {

  private val ConcurrentSteps = 10

  override def aggregateTags: Set[AggregateEventTag[TransferEvent]] = Tag.allTags

  override def buildHandler(): ReadSideProcessor.ReadSideHandler[TransferEvent] = new ReadSideProcessor.ReadSideHandler[TransferEvent] {

    override def handle(): Flow[EventStreamElement[TransferEvent], Done, NotUsed] = {
      Flow[EventStreamElement[TransferEvent]]
        .log("transferEvent")
        .withAttributes(
          Attributes.createLogLevels(
            Attributes.logLevelInfo,
            Attributes.logLevelInfo,
            Attributes.logLevelInfo
          )
        )
        .mapAsyncUnordered(ConcurrentSteps) { e =>
          e.event match {
            case evt: TransferInitiated => handleTransferInitiated(evt)
            case evt: CouldNotSecureFunds => handleCouldNotSecureFunds(evt)
            case evt: FundsRetrieved => handleFundsRetrieved(evt)
            case evt: DeliveryFailed => handleDeliveryFailed(evt)
            case evt: DeliveryConfirmed => handleDeliveryConfirmed(evt)
            case evt: RefundDelivered => handleRefundDelivered(evt)
          }
        }
    }

  }

  private def handleTransferInitiated(event: TransferInitiated): Future[Done] = {
    val transferEntity = transferRepository.get(event.transferId)
    event.transferDetails.source match {
      case portfolio: Account.Portfolio =>
        val transfer = Withdrawal(event.transferId, event.transferDetails.amount)
        portfolioService
          .processTransfer(portfolio.portfolioId)
          .invoke(transfer)
          .andThen {
            case Success(_) => transferEntity.ask(TransferCommand.RequestFundsSuccessful)
            case Failure(_) => transferEntity.ask(TransferCommand.RequestFundsFailed)
          }
      case _ =>
        // Any other sort of accounts are out of scope, this means they will freely accept and transfer money.
        // You don't actually want sources of free money in a production system.
        transferEntity.ask(TransferCommand.RequestFundsSuccessful)
    }
  }

  private def handleCouldNotSecureFunds(event: CouldNotSecureFunds): Future[Done] = {
    // Saga failed, but nothing to compensate for
    Future.successful(Done)
  }

  private def handleFundsRetrieved(event: FundsRetrieved): Future[Done] = {
    val transferEntity = transferRepository.get(event.transferId)
    event.transferDetails.source match {
      case portfolio: Account.Portfolio =>
        val transfer = Deposit(event.transferId, event.transferDetails.amount)
        portfolioService
          .processTransfer(portfolio.portfolioId)
          .invoke(transfer)
          .andThen {
            case Success(_) => transferEntity.ask(TransferCommand.DeliverySuccessful)
            case Failure(_) => transferEntity.ask(TransferCommand.DeliveryFailed)
          }
      case _ =>
        // As above, any unimplemented account type just freely accepts transfers
        transferEntity.ask(TransferCommand.DeliverySuccessful)
    }
  }

  private def handleDeliveryFailed(event: DeliveryFailed): Future[Done] = {
    val transferEntity = transferRepository.get(event.transferId)
    event.transferDetails.source match {
      case portfolio: Account.Portfolio =>
        val refund = Refund(event.transferId, event.transferDetails.amount)
        portfolioService
          .processTransfer(portfolio.portfolioId)
          .invoke(refund)
          .andThen {
            case Success(_) => transferEntity.ask(TransferCommand.RefundSuccessful)
            case Failure(_) => // not implemented
          }
      case _ =>
        transferEntity.ask(TransferCommand.RefundSuccessful)
    }

  }

  private def handleDeliveryConfirmed(event: DeliveryConfirmed): Future[Done] = {
    // Saga is completed successfully
    Future.successful(Done)
  }

  private def handleRefundDelivered(event: RefundDelivered): Future[Done] = {
    // Saga is complete after refunding source
    Future.successful(Done)
  }

}
