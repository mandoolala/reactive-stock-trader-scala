package stocktrader.portfolio.impl

import julienrf.json.derived
import play.api.libs.json._

import stocktrader.OrderId
import stocktrader.portfolio.api.LoyaltyLevel
import stocktrader.portfolio.api.LoyaltyLevel.LoyaltyLevel

sealed trait PortfolioState

object PortfolioState {

  case object Closed extends PortfolioState

  case class Open(funds: BigDecimal,
                  name: String,
                  loyaltyLevel: LoyaltyLevel,
                  holdings: Holdings,
                  activeOrders: Map[OrderId, PortfolioEvent.OrderPlaced],
                  completedOrders: Set[OrderId]) extends PortfolioState {

    def update(event: PortfolioEvent.FundsCredited): Open = this.copy(funds = this.funds + event.amount)
    def update(event: PortfolioEvent.FundsDebited): Open = this.copy(funds = this.funds - event.amount)

    def update(event: PortfolioEvent.SharesCredited): Open = this.copy(holdings = this.holdings.add(event.symbol, event.shares))
    def update(event: PortfolioEvent.SharesDebited): Open = this.copy(holdings = this.holdings.remove(event.symbol, event.shares))

    def update(event: PortfolioEvent.OrderPlaced): Open = this.copy(activeOrders = this.activeOrders + (event.orderId -> event))

    def orderCompleted(orderId: OrderId): Open = this.copy(
      activeOrders = this.activeOrders - orderId,
      completedOrders = this.completedOrders + orderId
    )

  }

  object Open {
    def initialState(name: String) = Open(
      funds = BigDecimal(0),
      name = name,
      loyaltyLevel = LoyaltyLevel.Bronze,
      holdings = Holdings.empty,
      activeOrders = Map.empty,
      completedOrders = Set.empty
    )
  }
  //implicit val format: Format[PortfolioState] = derived.flat.oformat((__ \ "type").format[String])
}
