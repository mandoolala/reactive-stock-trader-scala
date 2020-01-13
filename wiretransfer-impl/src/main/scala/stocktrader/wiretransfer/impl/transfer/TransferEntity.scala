package stocktrader.wiretransfer.impl

import java.text.SimpleDateFormat
import java.util.Date

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity
import com.lightbend.lagom.scaladsl.pubsub.{PubSubRef, PubSubRegistry, TopicId}
import org.slf4j.{Logger, LoggerFactory}
import stocktrader.TransferId
import stocktrader.wiretransfer.api.{Account, TransferCompleted}
import stocktrader.wiretransfer.impl.TransferState.Status
import stocktrader.wiretransfer.impl.transfer.TransferState

class TransferEntity(pubSubRegistry: PubSubRegistry) extends PersistentEntity {

  private final val log: Logger = LoggerFactory.getLogger(classOf[TransferEntity])

  override type Command = TransferCommand
  override type Event = TransferEvent
  override type State = Option[TransferState]

  val transferTopic: PubSubRef[TransferCompleted] = pubSubRegistry.refFor(TopicId[TransferCompleted]("transfer"))

  override def initialState: Option[TransferState] = None

  override def behavior: Behavior = {
    case None => empty
    case Some(TransferState(_, Status.FundsRequested)) => fundsRequested
    case Some(TransferState(_, Status.FundsSent)) => sendingFunds
    case Some(TransferState(_, Status.UnableToSecureFunds)) => fundsRequestFailed
    case Some(TransferState(_, Status.RefundSent)) => refundSent
    case Some(TransferState(_, Status.RefundDelivered)) => refundDelivered
  }

  private val empty = Actions()
    .onCommand[TransferCommand.TransferFunds, Done] {
      case (command: TransferCommand.TransferFunds, context, state) =>
        val transferDetails = TransferDetails(command.source, command.destination, command.amount)
        val transferCompleted = buildTransferCompleted(transferDetails, "Transfer Initiated")
        transferTopic.publish(transferCompleted)
        context.thenPersist(TransferEvent.TransferInitiated(getTransferId, transferDetails))(_ => context.reply(Done))
    }
    .onCommand[TransferCommand.RefundSuccessful.type, Done] {
      case (command, context, state) => warn(command, context, state)
    }
    .onCommand[TransferCommand.DeliveryFailed.type, Done] {
      case (command, context, state) => warn(command, context, state)
    }
    .onEvent {
      case (event: TransferEvent.TransferInitiated, None) =>
        Some(transfer.TransferState(event.transferDetails, Status.FundsRequested))
    }

  private val fundsRequested = Actions()
    .onCommand[TransferCommand.RequestFundsSuccessful.type, Done] {
      case (command, context, state) =>
        context.thenPersist(TransferEvent.FundsRetrieved(getTransferId, state.get.transferDetails))(_ => context.reply(Done))
    }
    .onCommand[TransferCommand.RequestFundsFailed.type, Done] {
      case (command, context, state) =>
        context.thenPersist(TransferEvent.CouldNotSecureFunds(getTransferId, state.get.transferDetails))(_ => context.reply(Done))
    }
    .onCommand[TransferCommand.RefundSuccessful.type, Done] {
      case (command, context, state) => warn(command, context, state)
    }
    .onCommand[TransferCommand.DeliveryFailed.type, Done] {
      case (command, context, state) => warn(command, context, state)
    }
    .onEvent {
      case (event: TransferEvent.FundsRetrieved, Some(state)) => Some(state.copy(status = Status.FundsSent))
      case (event: TransferEvent.CouldNotSecureFunds, Some(state)) => Some(state.copy(status = Status.UnableToSecureFunds))
    }

  private val sendingFunds = Actions()
    .onCommand[TransferCommand.DeliverySuccessful.type, Done] {
      case (command, context, Some(state)) =>
        val transferCompleted = buildTransferCompleted(state.transferDetails, "Delivery Confirmed")
        transferTopic.publish(transferCompleted)
        context.thenPersist(TransferEvent.DeliveryConfirmed(getTransferId, state.transferDetails))(_ => context.reply(Done))
    }
    .onCommand[TransferCommand.DeliveryFailed.type, Done] {
      case (command, context, Some(state)) =>
        context.thenPersist(TransferEvent.DeliveryFailed(getTransferId, state.transferDetails))(_ => context.reply(Done))
    }
    .onCommand[TransferCommand.RequestFundsSuccessful.type, Done] {
      case (command, context, state) => ignore(command, context, state)
    }
    .onCommand[TransferCommand.RefundSuccessful.type, Done] {
      case (command, context, state) => warn(command, context, state)
    }
    .onEvent {
      case (event: TransferEvent.DeliveryConfirmed, Some(state)) => Some(state.copy(status = Status.DeliveryConfirmed))
      case (event: TransferEvent.DeliveryFailed, Some(state)) => Some(state.copy(status = Status.RefundSent))
    }

  private val fundsRequestFailed = Actions()
    .onCommand[TransferCommand.RequestFundsFailed.type, Done] {
      case (command, context, state) => ignore(command, context, state)
    }
    .onCommand[TransferCommand.RefundSuccessful.type, Done] {
      case (command, context, state) => warn(command, context, state)
    }
    .onCommand[TransferCommand.DeliveryFailed.type, Done] {
      case (command, context, state) => warn(command, context, state)
    }

  private val refundSent = Actions()
    .onCommand[TransferCommand.RefundSuccessful.type, Done] {
      case (command, context, Some(state)) =>
        context.thenPersist(TransferEvent.RefundDelivered(getTransferId, state.transferDetails))(_ => context.reply(Done))
    }
    .onCommand[TransferCommand.RequestFundsSuccessful.type, Done] {
      case (command, context, state) => ignore(command, context, state)
    }
    .onCommand[TransferCommand.DeliveryFailed.type, Done] {
      case (command, context, state) => warn(command, context, state)
    }
    .onEvent {
      case (event: TransferEvent.RefundDelivered, Some(state)) => Some(state.copy(status = Status.RefundDelivered))
    }

  private val refundDelivered = Actions()
    .onCommand[TransferCommand.RefundSuccessful.type, Done] {
      case (command, context, state) => ignore(command, context, state)
    }
    .onCommand[TransferCommand.RequestFundsSuccessful.type, Done] {
      case (command, context, state) => warn(command, context, state)
    }
    .onCommand[TransferCommand.DeliveryFailed.type, Done] {
      case (command, context, state) => warn(command, context, state)
    }

  // ********* Helpers ********* \\
  private def getTransferId: TransferId = entityId

  private def done(context: CommandContext[Done]): Persist = {
    context.reply(Done)
    context.done
  }

  private def ignore(command: TransferCommand, context: CommandContext[Done], state: Option[TransferState]) = {
    log.warn(s"Ignoring command $command when state is $state")
    done(context)
  }

  private def warn(command: TransferCommand, context: CommandContext[Done], state: Option[TransferState]) = {
    log.warn(s"Command $command should never have been received when state is $state")
    done(context)
  }

  private def unhandled(command: TransferCommand, context: CommandContext[Done], state: Option[TransferState]) = {
    log.warn(s"Unhandled command $command when state is $state")
    done(context)
  }

  private def buildTransferCompleted(details: TransferDetails, status: String): TransferCompleted = {
    val dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
    val date = new Date

    val (sourceType, sourceId) = details.source match {
      case portfolio: Account.Portfolio => ("Portfolio", portfolio.portfolioId)
      case _ => ("Savings", "")
    }

    val (destType, destId) = details.destination match {
      case portfolio: Account.Portfolio => ("Portfolio", portfolio.portfolioId)
      case _ => ("Savings", "")
    }

    TransferCompleted(
      id = entityId,
      status = status,
      dateTime = dateFormat.format(date),
      sourceType = sourceType,
      sourceId = sourceId,
      destinationType = destType,
      destinationId = destId,
      amount = details.amount.toString()
    )
  }

}
