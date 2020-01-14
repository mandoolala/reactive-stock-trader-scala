package stocktrader.portfolio.impl

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.{PersistentEntityRef, PersistentEntityRegistry}

import stocktrader.PortfolioId
import stocktrader.broker.api.{OrderResult, Trade}
import stocktrader.{OrderId, PortfolioId}
import stocktrader.portfolio.api.PortfolioView
import stocktrader.portfolio.api.order.OrderDetails

import scala.concurrent.{ExecutionContext, Future}

/* Facade for a PortfolioModel. Wraps up all the logic surrounding an individual PortfolioEntity.
 * The PersistentEntity class itself can get large, so this wrapper can hold some of the logic around interactions with
 * the entity.
 */
class PortfolioModel(registry: PersistentEntityRegistry, portfolioId: PortfolioId)(implicit ec: ExecutionContext) {

  private val portfolioEntity: PersistentEntityRef[PortfolioCommand] = registry.refFor[PortfolioEntity](portfolioId)

  def view(): Future[PortfolioView] = {
    portfolioEntity.ask(PortfolioCommand.GetState).map { portfolio =>
      PortfolioView(
        portfolioId = portfolioId,
        name = portfolio.name,
        funds = portfolio.funds,
        holdings = portfolio.holdings.asSequence,
        completedOrders = portfolio.completedOrders.toSeq
      )
    }
  }

  def placeOrder(orderId: OrderId, orderDetails: OrderDetails): Future[Done] = {
    portfolioEntity.ask(PortfolioCommand.PlaceOrder(orderId, orderDetails))
  }

  def processTrade(orderId: OrderId, trade: Trade): Future[Done] = {
    portfolioEntity.ask(PortfolioCommand.CompleteTrade(orderId, trade))
  }

  def orderFailed(failed: OrderResult.Failed): Future[Done] = {
    portfolioEntity.ask(PortfolioCommand.AcknowledgeOrderFailure(failed))
  }

}
