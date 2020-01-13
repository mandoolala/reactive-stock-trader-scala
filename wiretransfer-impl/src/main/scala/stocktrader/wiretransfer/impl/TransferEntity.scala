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
      case (cmd: TransferCommand.TransferFunds, ctx, state) =>
        val transferDetails = TransferDetails(cmd.source, cmd.destination, cmd.amount)
        val transferCompleted = buildTransferCompleted(transferDetails, "Transfer Initiated")
        transferTopic.publish(transferCompleted)
        ctx.thenPersist(TransferEvent.TransferInitiated(getTransferId, transferDetails))(_ => ctx.reply(Done))
    }
    .onCommand[TransferCommand.RefundSuccessful.type, Done] {
      case (cmd, ctx, state) => warn(cmd, ctx, state)
    }
    .onCommand[TransferCommand.DeliveryFailed.type, Done] {
      case (cmd, ctx, state) => warn(cmd, ctx, state)
    }
    .onEvent {
      case (evt: TransferEvent.TransferInitiated, None) =>
        Some(TransferState(evt.transferDetails, Status.FundsRequested))
    }

  private val fundsRequested = Actions()
    .onCommand[TransferCommand.RequestFundsSuccessful.type, Done] {
      case (cmd, ctx, state) =>
        ctx.thenPersist(TransferEvent.FundsRetrieved(getTransferId, state.get.transferDetails))(_ => ctx.reply(Done))
    }
    .onCommand[TransferCommand.RequestFundsFailed.type, Done] {
      case (cmd, ctx, state) =>
        ctx.thenPersist(TransferEvent.CouldNotSecureFunds(getTransferId, state.get.transferDetails))(_ => ctx.reply(Done))
    }
    .onCommand[TransferCommand.RefundSuccessful.type, Done] {
      case (cmd, ctx, state) => warn(cmd, ctx, state)
    }
    .onCommand[TransferCommand.DeliveryFailed.type, Done] {
      case (cmd, ctx, state) => warn(cmd, ctx, state)
    }
    .onEvent {
      case (evt: TransferEvent.FundsRetrieved, Some(state)) => Some(state.copy(status = Status.FundsSent))
      case (evt: TransferEvent.CouldNotSecureFunds, Some(state)) => Some(state.copy(status = Status.UnableToSecureFunds))
    }

  private val sendingFunds = Actions()
    .onCommand[TransferCommand.DeliverySuccessful.type, Done] {
      case (cmd, ctx, Some(state)) =>
        val transferCompleted = buildTransferCompleted(state.transferDetails, "Delivery Confirmed")
        transferTopic.publish(transferCompleted)
        ctx.thenPersist(TransferEvent.DeliveryConfirmed(getTransferId, state.transferDetails))(_ => ctx.reply(Done))
    }
    .onCommand[TransferCommand.DeliveryFailed.type, Done] {
      case (cmd, ctx, Some(state)) =>
        ctx.thenPersist(TransferEvent.DeliveryFailed(getTransferId, state.transferDetails))(_ => ctx.reply(Done))
    }
    .onCommand[TransferCommand.RequestFundsSuccessful.type, Done] {
      case (cmd, ctx, state) => ignore(cmd, ctx, state)
    }
    .onCommand[TransferCommand.RefundSuccessful.type, Done] {
      case (cmd, ctx, state) => warn(cmd, ctx, state)
    }
    .onEvent {
      case (evt: TransferEvent.DeliveryConfirmed, Some(state)) => Some(state.copy(status = Status.DeliveryConfirmed))
      case (evt: TransferEvent.DeliveryFailed, Some(state)) => Some(state.copy(status = Status.RefundSent))
    }

  private val fundsRequestFailed = Actions()
    .onCommand[TransferCommand.RequestFundsFailed.type, Done] {
      case (cmd, ctx, state) => ignore(cmd, ctx, state)
    }
    .onCommand[TransferCommand.RefundSuccessful.type, Done] {
      case (cmd, ctx, state) => warn(cmd, ctx, state)
    }
    .onCommand[TransferCommand.DeliveryFailed.type, Done] {
      case (cmd, ctx, state) => warn(cmd, ctx, state)
    }

  private val refundSent = Actions()
    .onCommand[TransferCommand.RefundSuccessful.type, Done] {
      case (cmd, ctx, Some(state)) =>
        ctx.thenPersist(TransferEvent.RefundDelivered(getTransferId, state.transferDetails))(_ => ctx.reply(Done))
    }
    .onCommand[TransferCommand.RequestFundsSuccessful.type, Done] {
      case (cmd, ctx, state) => ignore(cmd, ctx, state)
    }
    .onCommand[TransferCommand.DeliveryFailed.type, Done] {
      case (cmd, ctx, state) => warn(cmd, ctx, state)
    }
    .onEvent {
      case (evt: TransferEvent.RefundDelivered, Some(state)) => Some(state.copy(status = Status.RefundDelivered))
    }

  private val refundDelivered = Actions()
    .onCommand[TransferCommand.RefundSuccessful.type, Done] {
      case (cmd, ctx, state) => ignore(cmd, ctx, state)
    }
    .onCommand[TransferCommand.RequestFundsSuccessful.type, Done] {
      case (cmd, ctx, state) => warn(cmd, ctx, state)
    }
    .onCommand[TransferCommand.DeliveryFailed.type, Done] {
      case (cmd, ctx, state) => warn(cmd, ctx, state)
    }

  // Helpers -----------------------------------------------------------------------------------------------------------

  private def getTransferId: TransferId = entityId

  private def done(ctx: CommandContext[Done]): Persist = {
    ctx.reply(Done)
    ctx.done
  }

  private def ignore(cmd: TransferCommand, ctx: CommandContext[Done], state: Option[TransferState]) = {
    log.warn(s"Ignoring command $cmd when state is $state")
    done(ctx)
  }

  private def warn(cmd: TransferCommand, ctx: CommandContext[Done], state: Option[TransferState]) = {
    log.warn(s"Command $cmd should never have been received when state is $state")
    done(ctx)
  }

  private def unhandled(cmd: TransferCommand, ctx: CommandContext[Done], state: Option[TransferState]) = {
    log.warn(s"Unhandled command $cmd when state is $state")
    done(ctx)
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
