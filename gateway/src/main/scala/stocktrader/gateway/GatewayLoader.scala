package stocktrader.gateway

import com.lightbend.lagom.scaladsl.api.{LagomConfigComponent, ServiceAcl, ServiceInfo}
import com.lightbend.lagom.scaladsl.client.LagomServiceClientComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.softwaremill.macwire._

import play.api.ApplicationLoader.Context
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.routing.Router
import play.api.{Application, ApplicationLoader, BuiltInComponentsFromContext, Mode}
import play.filters.HttpFiltersComponents
import play.filters.cors.{CORSComponents, CORSFilter}

import router.Routes
import stocktrader.portfolio.api.PortfolioService
import stocktrader.broker.api.BrokerService
import stocktrader.wiretransfer.api.WireTransferService

import services.quote.QuoteServiceImpl
import controllers.{AssetsComponents, PortfolioController, WireTransferController}

import scala.collection.immutable

class GatewayLoader extends ApplicationLoader {

  override def load(context: ApplicationLoader.Context): Application = context.environment.mode match {
    case Mode.Dev =>
      (new GatewayApplication(context) with LagomDevModeComponents).application
    case _ =>
      (new GatewayApplication(context) with LagomDevModeComponents).application
  }

}

abstract class GatewayApplication(context: Context) extends BuiltInComponentsFromContext(context)
  with AssetsComponents
  with HttpFiltersComponents
  with CORSComponents
  with AhcWSComponents
  with LagomConfigComponent
  with LagomServiceClientComponents {

  override lazy val serviceInfo: ServiceInfo = ServiceInfo(
    name = "gateway",
    locatableServices = Map(
      "gateway" -> immutable.Seq(
        // ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "(?!/api/).*"),
        ServiceAcl.forPathRegex("(?!/api/).*")
      )
    ))
  override lazy val router: Router = {
    val prefix = "/"
    wire[Routes]
  }
  override lazy val httpFilters: Seq[CORSFilter] = Seq(corsFilter)


  lazy val portfolioService = serviceClient.implement[PortfolioService]
  lazy val brokerService = serviceClient.implement[BrokerService]
  lazy val wireTransferService = serviceClient.implement[WireTransferService]

  lazy val quoteService = wire[QuoteServiceImpl]

  lazy val portfolioController = wire[PortfolioController]
  lazy val wireTransferController = wire[WireTransferController]

}
