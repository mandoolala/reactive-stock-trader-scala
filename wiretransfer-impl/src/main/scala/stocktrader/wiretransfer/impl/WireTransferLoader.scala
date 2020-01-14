package stocktrader.wiretransfer.impl

import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.api.{Descriptor, ServiceLocator}
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.pubsub.PubSubComponents
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomApplicationLoader, LagomServer}
import com.softwaremill.macwire._
import play.api.libs.ws.ahc.AhcWSComponents
import stocktrader.portfolio.api.PortfolioService
import stocktrader.wiretransfer.api.WireTransferService
import stocktrader.wiretransfer.impl.transfer.{TransferEntity, TransferEventProcessor, TransferProcess, TransferRepositoryImpl}

class WireTransferLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new WireTransferApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new WireTransferApplication(context) with LagomDevModeComponents

  override def describeService: Option[Descriptor] = Some(readDescriptor[WireTransferService])

}

abstract class WireTransferApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with PubSubComponents
    with AhcWSComponents {

  override lazy val lagomServer: LagomServer = serverFor[WireTransferService](wire[WireTransferServiceImpl])
  override lazy val jsonSerializerRegistry = WireTransferSerializerRegistry

  persistentEntityRegistry.register(wire[TransferEntity])

  readSide.register(wire[TransferEventProcessor])
  readSide.register(wire[TransferProcess])

  lazy val portfolioService = serviceClient.implement[PortfolioService]
  lazy val transferRepository = wire[TransferRepositoryImpl]

}
