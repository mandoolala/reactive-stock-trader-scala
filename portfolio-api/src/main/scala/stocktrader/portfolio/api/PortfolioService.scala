package stocktrader.portfolio.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import stocktrader.{OrderId, PortfolioId}
import stocktrader.portfolio.api.order.OrderDetails

trait PortfolioService extends Service {

  import Service._

  def openPortfolio(): ServiceCall[OpenPortfolioDetails, PortfolioId]
  def closePortfolio(portfolioId: PortfolioId): ServiceCall[NotUsed, Done]
  def getPortfolio(portfolioId: PortfolioId): ServiceCall[NotUsed, PortfolioView]
  def getAllPortfolios(): ServiceCall[NotUsed, Seq[PortfolioSummary]]
  def placeOrder(portfolioId: PortfolioId): ServiceCall[OrderDetails, OrderId]
  def processTransfer(portfolioId: PortfolioId): ServiceCall[FundsTransfer, Done]

  final val OrderPlacedTopicId = "Portfolio-OrderPlaced"
  def orderPlaced(): Topic[OrderPlaced]

  override def descriptor: Descriptor = {
    named("portfolio-service")
      .withCalls(
        restCall(Method.POST, "/api/portfolio", openPortfolio _),
        restCall(Method.POST, "/api/portfolio/:portfolioId/close", closePortfolio _),
        restCall(Method.GET, "/api/portfolio/:portfolioId", getPortfolio _),
        restCall(Method.GET, "/api/portfolio", getAllPortfolios _),
        restCall(Method.POST, "/api/portfolio/:portfolioId/placeOrder", placeOrder _),
        restCall(Method.POST, "/api/portfolio/:portfolio/processTransfer", processTransfer _)

      )
      .withTopics(
      topic(OrderPlacedTopicId, orderPlaced _)
    )
  }

}