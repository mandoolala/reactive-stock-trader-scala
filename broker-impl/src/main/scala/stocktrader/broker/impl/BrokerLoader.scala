package stocktrader.broker.impl

import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.api.{Descriptor, ServiceLocator}
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomApplicationLoader, LagomServer}
import com.softwaremill.macwire._
import play.api.libs.ws.ahc.AhcWSComponents
import stocktrader.broker.api.BrokerService
import stocktrader.broker.impl.order.{OrderEntity, OrderRepositoryImpl}
import stocktrader.broker.impl.quote.IexQuoteServiceImpl
import stocktrader.broker.impl.trade.TradeServiceImpl
import stocktrader.portfolio.api.PortfolioService

class BrokerLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new BrokerApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new BrokerApplication(context) with LagomDevModeComponents

  override def describeService: Option[Descriptor] = Some(readDescriptor[BrokerService])

}

abstract class BrokerApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with AhcWSComponents {

  override lazy val lagomServer: LagomServer = serverFor[BrokerService](wire[BrokerServiceImpl])
  override lazy val jsonSerializerRegistry = BrokerSerializerRegistry

  persistentEntityRegistry.register(wire[OrderEntity])

  //readSide.register(wire[BrokerEventProcessor])

  lazy val portfolioService = serviceClient.implement[PortfolioService]
  lazy val orderRepository = wire[OrderRepositoryImpl]
  lazy val quoteService = wire[IexQuoteServiceImpl]
  lazy val tradeService = wire[TradeServiceImpl]

}
