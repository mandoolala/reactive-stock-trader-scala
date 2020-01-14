package stocktrader.portfolio.impl

import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.api.{Descriptor, ServiceLocator}
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomApplicationLoader, LagomServer}
import com.softwaremill.macwire._
import play.api.libs.ws.ahc.AhcWSComponents
import stocktrader.broker.api.BrokerService
import stocktrader.portfolio.api.PortfolioService

class PortfolioLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new PortfolioApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new PortfolioApplication(context) with LagomDevModeComponents

  override def describeService: Option[Descriptor] = Some(readDescriptor[PortfolioService])

}

abstract class PortfolioApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with AhcWSComponents {

  override lazy val lagomServer: LagomServer = serverFor[PortfolioService](wire[PortfolioServiceImpl])
  override lazy val jsonSerializerRegistry = PortfolioSerializerRegistry

  persistentEntityRegistry.register(wire[PortfolioEntity])

  readSide.register(wire[PortfolioEventProcessor])

  lazy val brokerService = serviceClient.implement[BrokerService]
  lazy val portfolioRepository = wire[PortfolioRepositoryImpl]

}
