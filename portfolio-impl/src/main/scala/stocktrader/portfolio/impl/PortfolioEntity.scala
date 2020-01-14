package stocktrader.portfolio.impl

import akka.Done
import com.lightbend.lagom.scaladsl.api.transport.NotFound
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity

import org.slf4j.{Logger, LoggerFactory}

import stocktrader.TradeType

class PortfolioEntity extends PersistentEntity {

  private final val log: Logger = LoggerFactory.getLogger(classOf[PortfolioEntity])

  override type Command = PortfolioCommand
  override type Event = PortfolioEvent
  override type State = Option[PortfolioState]

  override def initialState: Option[PortfolioState] = None

  override def behavior: Behavior = {
    case None => uninitialized
    case Some(_: PortfolioState.Open) => open
    case Some(_: PortfolioState.Closed.type) => closed
  }

  private val uninitialized = Actions()
    .onCommand[PortfolioCommand.Open, Done] {
      case (command: PortfolioCommand.Open, context, state) =>
        val openEvent = PortfolioEvent.Opened(portfolioId, command.name)
        log.info(openEvent.toString)
        context.thenPersist(openEvent)(_ => context.reply(Done))
    }
    .onReadOnlyCommand[PortfolioCommand.GetState.type, PortfolioState.Open] {
      case (command, context, state) =>
        context.commandFailed(NotFound(s"Portfolio $portfolioId not found"))
    }
    .onEvent {
      case (event: PortfolioEvent.Opened, state) =>
        log.info(s"Opened $entityId, named ${event.name}")
        Some(PortfolioState.Open.initialState(event.name))
    }

  private val open = Actions()
    .onCommand[PortfolioCommand.Open, Done] {
      case (command: PortfolioCommand.Open, context, state) => rejectOpen(command, context)
    }
    .onCommand[PortfolioCommand.PlaceOrder, Done] {
      case (command: PortfolioCommand.PlaceOrder, context, Some(state: PortfolioState.Open)) => placeOrder(command, context, state)
    }
    .onCommand[PortfolioCommand.CompleteTrade, Done] {
      case (command: PortfolioCommand.CompleteTrade, context, Some(state: PortfolioState.Open)) => completeTrade(command, context, state)
    }
    .onCommand[PortfolioCommand.AcknowledgeOrderFailure, Done] {
      case (command: PortfolioCommand.AcknowledgeOrderFailure, context, Some(state: PortfolioState.Open)) => handleFailedOrder(command, context, state)
    }
    .onCommand[PortfolioCommand.SendFunds, Done] {
      case (command: PortfolioCommand.SendFunds, context, Some(state: PortfolioState.Open)) => sendFunds(command, context, state)
    }
    .onCommand[PortfolioCommand.ReceiveFunds, Done] {
      case (command: PortfolioCommand.ReceiveFunds, context, Some(state: PortfolioState.Open)) => receiveFunds(command, context)
    }
    .onCommand[PortfolioCommand.AcceptRefund, Done] {
      case (command: PortfolioCommand.AcceptRefund, context, Some(state: PortfolioState.Open)) => acceptRefund(command, context)
    }
    .onCommand[PortfolioCommand.ClosePortfolio.type, Done] {
      case (PortfolioCommand.ClosePortfolio, context, Some(state: PortfolioState.Open)) => closePortfolio(context, state)
    }
    .onReadOnlyCommand[PortfolioCommand.GetState.type, PortfolioState.Open] {
      case (PortfolioCommand.GetState, context, Some(state: PortfolioState.Open)) => getState(context, state)
    }
    .onEvent {
      case (event: PortfolioEvent.OrderPlaced, Some(state: PortfolioState.Open)) => Some(state.update(event))
      case (event: PortfolioEvent.SharesCredited, Some(state: PortfolioState.Open)) => Some(state.update(event))
      case (event: PortfolioEvent.FundsDebited, Some(state: PortfolioState.Open)) => Some(state.update(event))
      case (event: PortfolioEvent.FundsCredited, Some(state: PortfolioState.Open)) => Some(state.update(event))
      case (event: PortfolioEvent.SharesDebited, Some(state: PortfolioState.Open)) => Some(state.update(event))
      case (event: PortfolioEvent.OrderFulfilled, Some(state: PortfolioState.Open)) => Some(state.orderCompleted(event.orderId))
      case (event: PortfolioEvent.OrderFailed, Some(state: PortfolioState.Open)) => Some(state.orderCompleted(event.orderId))
    }

  private val closed = {
    ???
    Actions()
  }

  private def rejectOpen(command: PortfolioCommand.Open, context: CommandContext[Done]): Persist = {
    context.commandFailed(new PortfolioAlreadyOpened(portfolioId))
    context.done
  }

  private def placeOrder(command: PortfolioCommand.PlaceOrder, context: CommandContext[Done], state: PortfolioState.Open): Persist = {
    log.info(s"Placing order $command")
    val orderDetails = command.orderDetails
    orderDetails.tradeType match {
      case TradeType.Sell =>
        val available = state.holdings.getShareCount(orderDetails.symbol)
        if (available >= orderDetails.shares) {
          context.thenPersistAll(
            PortfolioEvent.OrderPlaced(command.orderId, portfolioId, command.orderDetails),
            PortfolioEvent.SharesDebited(portfolioId, orderDetails.symbol, orderDetails.shares)
          )(() => context.reply(Done))
        } else {
          context.commandFailed(new InsufficientShares(
            s"Insufficient shares of ${orderDetails.symbol} for sell, ${orderDetails.shares} required, $available held."
          ))
          context.done
        }
      case TradeType.Buy =>
        context.thenPersist(PortfolioEvent.OrderPlaced(command.orderId, portfolioId, command.orderDetails))(_ => context.reply(Done))
      case _ => throw new IllegalStateException()
    }
  }
  private def completeTrade(command: PortfolioCommand.CompleteTrade, context: CommandContext[Done], state: PortfolioState.Open): Persist = {

    val trade = command.trade
    val orderId = command.orderId

    log.info(s"PortfolioModel $portfolioId processing trade $trade")

    state.activeOrders.get(orderId) match {

      case None => done(context)
        // This is a trade for an order that we don't believe to be active, presumably we've already processed
        // the result of this order and this is a duplicate result.
        // TODO: More complete tracking so we know that we're not dropping trades

      case Some(orderPlaced) =>

        trade.tradeType match {

          case TradeType.Buy =>
            context.thenPersistAll(
              PortfolioEvent.FundsDebited(portfolioId, trade.sharePrice),
              PortfolioEvent.SharesCredited(portfolioId, trade.symbol, trade.shares),
              PortfolioEvent.OrderFulfilled(portfolioId, orderId)
            )(() => context.reply(Done))

          case TradeType.Sell =>
            context.thenPersistAll(
              PortfolioEvent.FundsCredited(portfolioId, trade.sharePrice),
              PortfolioEvent.OrderFulfilled(portfolioId, orderId)
            )(() => context.reply(Done))

          case _ => throw new IllegalStateException()
        }
    }
  }

  private def handleFailedOrder(command: PortfolioCommand.AcknowledgeOrderFailure, context: CommandContext[Done], state: PortfolioState.Open): Persist = {

    val orderId = command.orderFailed.orderId

    log.info(s"Order $orderId failed for PortfolioModel $portfolioId")

    state.activeOrders.get(orderId) match {
      case None =>
        // Not currently an active order, this may be a duplicate message.
        log.info(s"Order failure for order $orderId, which is not currently active.")
        done(context)
      case Some(orderPlaced) =>
        log.info(s"Order failure for order $orderId.")
        orderPlaced.orderDetails.tradeType match {
          case TradeType.Sell =>
            context.thenPersistAll(
              PortfolioEvent.SharesCredited(portfolioId, orderPlaced.orderDetails.symbol, orderPlaced.orderDetails.shares),
              PortfolioEvent.OrderFailed(portfolioId, orderId)
            )(() => context.reply(Done))
          case TradeType.Buy =>
            context.thenPersist(PortfolioEvent.OrderFailed(portfolioId, orderId))(_ => context.reply(Done))
          case _ => throw new IllegalStateException()
        }
    }
  }

  private def sendFunds(command: PortfolioCommand.SendFunds, context: CommandContext[Done], state: PortfolioState.Open): Persist = {
    if (state.funds >= command.amount) {
      context.thenPersist(PortfolioEvent.FundsDebited(portfolioId, command.amount))(_ => context.reply(Done))
    } else {
      context.commandFailed(new InsufficientFunds(s"Attempt to send ${command.amount}, but only ${state.funds} available."))
      context.done
    }
  }

  private def receiveFunds(command: PortfolioCommand.ReceiveFunds, context: CommandContext[Done]): Persist = {
    context.thenPersist(PortfolioEvent.FundsCredited(portfolioId, command.amount))(_ => context.reply(Done))
  }

  private def acceptRefund(command: PortfolioCommand.AcceptRefund, context: CommandContext[Done]): Persist = {
    context.thenPersist(PortfolioEvent.RefundAccepted(portfolioId, command.transferId, command.amount))(_ => context.reply(Done))
  }

  private def closePortfolio(context: CommandContext[Done], state: PortfolioState.Open): Persist = {
    if (isEmpty(state)) {
      context.thenPersist(PortfolioEvent.Closed(portfolioId))(_ => context.reply(Done))
    } else {
      context.commandFailed(new IllegalStateException("Portfolio is not empty"))
      context.done
    }
  }
  private def getState(context: ReadOnlyCommandContext[PortfolioState.Open], state: PortfolioState.Open): Unit = {
    context.reply(state)
  }

  // ********* Helpers ********* \\
  private def portfolioId: String = entityId

  private def done(context: CommandContext[Done]): Persist = {
    context.reply(Done)
    context.done
  }

  private def isEmpty(state: PortfolioState.Open): Boolean = {
    state.funds == 0 && state.holdings.asSequence.isEmpty && state.activeOrders.isEmpty
  }

}
