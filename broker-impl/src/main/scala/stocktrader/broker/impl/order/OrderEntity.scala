package stocktrader.broker.impl.order


import akka.Done

import com.lightbend.lagom.scaladsl.persistence.PersistentEntity
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.InvalidCommandException

import org.slf4j.{Logger, LoggerFactory}

import stocktrader.OrderId
import stocktrader.broker.api.{OrderResult, OrderStatus, OrderSummary}
import stocktrader.portfolio.api.order.Order


class OrderEntity extends PersistentEntity {

  private final val log: Logger = LoggerFactory.getLogger(classOf[OrderEntity])

  override type Command = OrderCommand
  override type Event = OrderEvent
  override type State = Option[OrderState]

  override def initialState: Option[OrderState] = None

  override def behavior: Behavior = {
    case None => uninitialized
    case Some(_: OrderState.Pending) => pending
    case Some(_: OrderState.Fulfilled) => fulfilled
    case Some(_: OrderState.Failed) => failed
  }

  private val common = Actions()
    .onReadOnlyCommand[OrderCommand.GetStatus.type, Option[OrderStatus]] {
      case (command, context, state) =>
        context.reply(state.map(_.status))
    }
    .onReadOnlyCommand[OrderCommand.GetSummary.type, Option[OrderSummary]] {
      case (command, context, maybeState) =>
        context.reply(maybeState.map { state =>
          OrderSummary(
            orderId = getOrderId,
            portfolioId = state.portfolioId,
            tradeType = state.orderDetails.tradeType,
            symbol = state.orderDetails.symbol,
            shares = state.orderDetails.shares,
            status = state.status
          )
        })
    }
    .onReadOnlyCommand[OrderCommand.PlaceOrder, Order] {
      case (command: OrderCommand.PlaceOrder, context, maybeState) =>
        maybeState.map { state =>
          val orderDetails = command.orderDetails
          if (orderDetails == state.orderDetails) {
            context.reply(getOrder(state))
          } else {
            log.info(s"Order $getOrderId, existing: ${state.orderDetails}, received: $orderDetails")
            context.commandFailed(InvalidCommandException(s"Attempt to place different order with same order ID: $getOrderId"))
          }
        }
    }

  private val uninitialized = Actions()
    .onCommand[OrderCommand.PlaceOrder, Order] {
      case (command: OrderCommand.PlaceOrder, context, state) =>
        val order = Order(getOrderId, command.portfolioId, command.orderDetails)
        context.thenPersist(OrderEvent.OrderReceived(order))(_ => context.reply(order))
    }
    .onEvent {
      case (evt: OrderEvent.OrderReceived, state) =>
        Some(OrderState.Pending(evt.order.portfolioId, evt.order.details))
    }
    .onReadOnlyCommand[OrderCommand.GetSummary.type, Option[OrderSummary]] {
      case (command, context, state) =>
        context.reply(None) // return state?
    }

  private val pending = Actions()
    .onCommand[OrderCommand.CompleteOrder, Done] {
      case (command: OrderCommand.CompleteOrder, context, state) =>
        command.orderResult match {
          case orderFulfilled: OrderResult.Fulfilled =>
            context.thenPersist(OrderEvent.OrderFulfilled(getOrder(state.get), orderFulfilled.trade))(_ => context.reply(Done))
          case orderFailed: OrderResult.Failed =>
            context.thenPersist(OrderEvent.OrderFailed(getOrder(state.get)))(_ => context.reply(Done))
        }
    }
    .onEvent {
      case (evt: OrderEvent.OrderFulfilled, state) =>
        // get details from event?
        Some(OrderState.Fulfilled(state.get.portfolioId, state.get.orderDetails, evt.trade.sharePrice))
      case (evt: OrderEvent.OrderFailed, state) =>
        Some(OrderState.Failed(evt.order.portfolioId, evt.order.details))
    }
    .orElse(common)

  private val fulfilled = common
  private val failed = common

  // ********* Helpers ********* \\

  private def getOrderId: OrderId = OrderId(entityId)
  private def getOrder(state: OrderState): Order = Order(getOrderId, state.portfolioId, state.orderDetails)

}
