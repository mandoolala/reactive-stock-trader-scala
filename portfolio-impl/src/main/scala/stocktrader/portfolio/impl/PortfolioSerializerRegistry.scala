package stocktrader.portfolio.impl

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

object PortfolioSerializerRegistry extends JsonSerializerRegistry {
  override def serializers = List(
    // State
    JsonSerializer[PortfolioState.Open],
    JsonSerializer[PortfolioState.Closed.type],
    // Commands
    JsonSerializer[PortfolioCommand.Liquidate.type],
    JsonSerializer[PortfolioCommand.GetState.type],
    JsonSerializer[PortfolioCommand.Open],
    JsonSerializer[PortfolioCommand.PlaceOrder],
    JsonSerializer[PortfolioCommand.CompleteTrade],
    JsonSerializer[PortfolioCommand.ReceiveFunds],
    JsonSerializer[PortfolioCommand.SendFunds],
    JsonSerializer[PortfolioCommand.AcceptRefund],
    JsonSerializer[PortfolioCommand.AcknowledgeOrderFailure],
    JsonSerializer[PortfolioCommand.ClosePortfolio.type],
    // Events
    JsonSerializer[PortfolioEvent.Opened],
    JsonSerializer[PortfolioEvent.LiquidationStarted],
    JsonSerializer[PortfolioEvent.Closed],
    JsonSerializer[PortfolioEvent.SharesCredited],
    JsonSerializer[PortfolioEvent.SharesDebited],
    JsonSerializer[PortfolioEvent.FundsDebited],
    JsonSerializer[PortfolioEvent.FundsCredited],
    JsonSerializer[PortfolioEvent.RefundAccepted],
    JsonSerializer[PortfolioEvent.OrderPlaced],
    JsonSerializer[PortfolioEvent.OrderFulfilled],
    JsonSerializer[PortfolioEvent.OrderFailed],
  )
}
