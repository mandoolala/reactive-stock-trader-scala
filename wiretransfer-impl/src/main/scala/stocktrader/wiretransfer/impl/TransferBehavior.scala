package stocktrader.wiretransfer.impl

import java.text.SimpleDateFormat
import java.util.Date

import akka.Done
import org.slf4j.{Logger, LoggerFactory}
import stocktrader.wiretransfer.api.{Account, TransferCompleted}
import stocktrader.wiretransfer.impl.TransferState.Status

trait TransferBehavior {
  self: TransferEntity =>

  private final val log: Logger = LoggerFactory.getLogger(classOf[TransferBehavior])

  def onTransferFunds(cmd: TransferCommand.TransferFunds, ctx: CommandContext[Done], state: Option[TransferState]): Persist = {
    state match {
      case None =>
        val transferDetails = TransferDetails(cmd.source, cmd.destination, cmd.amount)
        val transferCompleted = buildTransferCompleted(transferDetails, "Transfer Initiated")
        transferTopic.publish(transferCompleted)
        ctx.thenPersist(TransferEvent.TransferInitiated(getTransferId, transferDetails))(_ => ctx.reply(Done))
      case Some(s) => s.status match {
        case Status.FundsRequested => unhandled(cmd, state, ctx)
        case Status.UnableToSecureFunds => unhandled(cmd, state, ctx)
        case Status.FundsSent => ???
        case Status.DeliveryConfirmed => ???
        case Status.RefundSent => ???
        case Status.RefundDelivered => ???
      }
    }
  }

  def onRequestFundsSuccessful(cmd: TransferCommand.RequestFundsSuccessful.type, ctx: CommandContext[Done], state: Option[TransferState]): Persist = {
    state match {
      case None => unhandled(cmd, state, ctx)
      case Some(s) => s.status match {
        case Status.FundsRequested =>
          ctx.thenPersist(TransferEvent.FundsRetrieved(getTransferId, s.transferDetails))(_ => ctx.reply(Done))
        case Status.UnableToSecureFunds => unhandled(cmd, state, ctx)
        case Status.FundsSent => ???
        case Status.DeliveryConfirmed => ???
        case Status.RefundSent => ???
        case Status.RefundDelivered => ???
      }
    }
  }

  def onRequestFundsFailed(cmd: TransferCommand.RequestFundsFailed.type, ctx: CommandContext[Done], state: Option[TransferState]): Persist = {
    state match {
      case None => unhandled(cmd, state, ctx)
      case Some(s) => s.status match {
        case Status.FundsRequested =>
          ctx.thenPersist(TransferEvent.CouldNotSecureFunds(getTransferId, s.transferDetails))(_ => ctx.reply(Done))
        case Status.UnableToSecureFunds => ignore(cmd, state, ctx)
        case Status.FundsSent => ???
        case Status.DeliveryConfirmed => ???
        case Status.RefundSent => ???
        case Status.RefundDelivered => ???
      }
    }
  }

  def onDeliveryFailed(cmd: TransferCommand.DeliveryFailed.type, ctx: CommandContext[Done], state: Option[TransferState]): Persist = {
    state match {
      case None => warn(cmd, state, ctx)
      case Some(s) => s.status match {
        case Status.FundsRequested => warn(cmd, state, ctx)
        case Status.UnableToSecureFunds => warn(cmd, state, ctx)
        case Status.FundsSent => ???
        case Status.DeliveryConfirmed => ???
        case Status.RefundSent => ???
        case Status.RefundDelivered => ???
      }
    }
  }

  def onDeliverySuccessful(cmd: TransferCommand.DeliverySuccessful.type, ctx: CommandContext[Done], state: Option[TransferState]): Persist = {
    state match {
      case None => unhandled(cmd, state, ctx)
      case Some(s) => s.status match {
        case Status.FundsRequested => unhandled(cmd, state, ctx)
        case Status.UnableToSecureFunds => unhandled(cmd, state, ctx)
        case Status.FundsSent => ???
        case Status.DeliveryConfirmed => ???
        case Status.RefundSent => ???
        case Status.RefundDelivered => ???
      }
    }
  }

  def onRefundSuccessful(cmd: TransferCommand.RefundSuccessful.type, ctx: CommandContext[Done], state: Option[TransferState]): Persist = {
    state match {
      case None => warn(cmd, state, ctx)
      case Some(s) => s.status match {
        case Status.FundsRequested => warn(cmd, state, ctx)
        case Status.UnableToSecureFunds => warn(cmd, state, ctx)
        case Status.FundsSent => ???
        case Status.DeliveryConfirmed => ???
        case Status.RefundSent => ???
        case Status.RefundDelivered => ???
      }
    }
  }

  // Events ------------------------------------------------------------------------------------------------------------

  def onTransferInitiated(evt: TransferEvent.TransferInitiated, state: Option[TransferState]): Option[TransferState] = {
    Some(TransferState(evt.transferDetails, Status.FundsRequested))
  }

  def onFundsRetrieved(evt: TransferEvent.FundsRetrieved, state: Option[TransferState]): Option[TransferState] = {
    state match {
      case None => unhandled(evt, state)
      case Some(state) => state.status match {
        case Status.FundsRequested => Some(state.copy(status = Status.FundsSent))
        case Status.UnableToSecureFunds => ???
        case Status.FundsSent => ???
        case Status.DeliveryConfirmed => ???
        case Status.RefundSent => ???
        case Status.RefundDelivered => ???
      }
    }
  }

  def onCouldNotSecureFunds(evt: TransferEvent.CouldNotSecureFunds, state: Option[TransferState]): Option[TransferState] = {
    state match {
      case None => unhandled(evt, state)
      case Some(state) => state.status match {
        case Status.FundsRequested => Some(state.copy(status = Status.UnableToSecureFunds))
        case Status.UnableToSecureFunds => ???
        case Status.FundsSent => ???
        case Status.DeliveryConfirmed => ???
        case Status.RefundSent => ???
        case Status.RefundDelivered => ???
      }
    }
  }

  def onDeliveryConfirmed(evt: TransferEvent.DeliveryConfirmed, state: Option[TransferState]): Option[TransferState] = {
    state match {
      case None => unhandled(evt, state)
      case Some(state) => state.status match {
        case Status.FundsRequested => ???
        case Status.UnableToSecureFunds => ???
        case Status.FundsSent => ???
        case Status.DeliveryConfirmed => ???
        case Status.RefundSent => ???
        case Status.RefundDelivered => ???
      }
    }
  }

  def onDeliveryFailed(evt: TransferEvent.DeliveryFailed, state: Option[TransferState]): Option[TransferState] = {
    state match {
      case None => unhandled(evt, state)
      case Some(state) => state.status match {
        case Status.FundsRequested => ???
        case Status.UnableToSecureFunds => ???
        case Status.FundsSent => ???
        case Status.DeliveryConfirmed => ???
        case Status.RefundSent => ???
        case Status.RefundDelivered => ???
      }
    }
  }

  def onRefundDelivered(evt: TransferEvent.RefundDelivered, state: Option[TransferState]): Option[TransferState] = {
    state match {
      case None => unhandled(evt, state)
      case Some(state) => state.status match {
        case Status.FundsRequested => ???
        case Status.UnableToSecureFunds => ???
        case Status.FundsSent => ???
        case Status.DeliveryConfirmed => ???
        case Status.RefundSent => ???
        case Status.RefundDelivered => ???
      }
    }
  }

  // Command handlers --------------------------------------------------------------------------------------------------

  private def ignore(cmd: TransferCommand, state: Option[TransferState], ctx: CommandContext[Done]) = {
    log.warn(s"Ignoring command $cmd when state is $state")
    done(ctx)
  }

  private def warn(cmd: TransferCommand, state: Option[TransferState], ctx: CommandContext[Done]) = {
    log.warn(s"Command $cmd should never have been received when state is $state")
    done(ctx)
  }

  private def unhandled(cmd: TransferCommand, state: Option[TransferState], ctx: CommandContext[Done]) = {
    log.warn(s"Unhandled command $cmd when state is $state")
    done(ctx)
  }

  // Event handlers ----------------------------------------------------------------------------------------------------

  private def fundsRequested(evt: TransferEvent.TransferInitiated) = {

  }

  private def unhandled(evt: TransferEvent, state: Option[TransferState]) = {
    log.warn(s"Unhandled event $evt when state is $state")
    state
  }

  // Helpers -----------------------------------------------------------------------------------------------------------

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

  private def done(ctx: CommandContext[Done]): Persist = {
    ctx.reply(Done)
    ctx.done
  }

}

