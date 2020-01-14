package controllers

import org.slf4j.{Logger, LoggerFactory}

import play.api.i18n._
import play.api.libs.json.Json
import play.api.mvc._

import services.quote.QuoteService
import stocktrader.broker.api.{BrokerService, OrderStatus, OrderSummary}
import stocktrader.portfolio.api
import stocktrader.portfolio.api.order.OrderDetails
import stocktrader.portfolio.api.{OpenPortfolioDetails, PortfolioService}
import stocktrader.{OrderId, PortfolioId}

import controllers.forms.portfolio.{OpenPortfolioForm, PlaceOrderForm}
import models.{CompletedOrder, EquityHolding, Portfolio, PortfolioSummary}

import scala.concurrent.{ExecutionContext, Future}

class PortfolioController(cc: ControllerComponents,
                          portfolioService: PortfolioService,
                          quoteService: QuoteService,
                          brokerService: BrokerService,
                         )(implicit ec: ExecutionContext)
  extends AbstractController(cc) with I18nSupport {

  private final val log: Logger = LoggerFactory.getLogger(classOf[PortfolioController])

  def getPortfolio(portfolioId: String) = Action.async { _ =>
    val pricedView: Future[Portfolio] = for {
      portfolioView <- portfolioService.getPortfolio(portfolioId).invoke
      pricedHoldings <- quoteService.priceHoldings(portfolioView.holdings)
    } yield {
      Portfolio(portfolioView.portfolioId, portfolioView.name, portfolioView.funds, pricedHoldings)
    }
    pricedView.map(Json.toJson[Portfolio]).map(Ok(_))
  }

  def getAllPortfolios() = Action.async { _ =>
    val portfolios: Future[Seq[api.PortfolioSummary]] = portfolioService.getAllPortfolios().invoke
    portfolios.map(Json.toJson[Seq[api.PortfolioSummary]]).map(Ok(_))
  }

  def getSummary(portfolioId: PortfolioId, includeOrderInfo: Boolean, includePrices: Boolean) = Action.async { _ =>
    val summaryView: Future[PortfolioSummary] = for {
      model <- portfolioService.getPortfolio(portfolioId).invoke
      completedOrders <- {
        if (includeOrderInfo) completedOrders(model.completedOrders)
        else Future.successful(Seq.empty)
      }
    } yield {
      val equityHoldings = {
        if (includePrices) model.holdings.map { holding =>
          EquityHolding(
            symbol = holding.symbol,
            shares = holding.shareCount,
            sharePrice = 0
          )
        }
        else Seq.empty
      }
      PortfolioSummary(
        portfolioId = model.portfolioId,
        name = model.name,
        funds = model.funds,
        equities = equityHoldings,
        completedOrders = completedOrders
      )
    }
    summaryView.map(Json.toJson[PortfolioSummary]).map(Ok(_))
  }

  def openPortfolio() = Action.async { implicit request =>
    val form = OpenPortfolioForm.form.bindFromRequest
    if (form.hasErrors) {
      Future.successful(BadRequest(form.errorsAsJson))
    } else {
      val openRequest: OpenPortfolioDetails = form.get.toRequest
      portfolioService.openPortfolio().invoke(openRequest)
        .map { portfolioId =>
          Json.obj("portfolioId" -> portfolioId)
        }
        .map(Created(_))
    }
  }

  def placeOrder(portfolioId: String) = Action.async { implicit request =>
    val form = PlaceOrderForm.form.bindFromRequest
    if (form.hasErrors) {
      Future.successful(BadRequest(form.errorsAsJson))
    } else {
      val placeOrderForm: PlaceOrderForm = form.get
      val orderDetails: OrderDetails = placeOrderForm.toOrderDetails
      portfolioService.placeOrder(portfolioId).invoke(orderDetails)
        .map { orderId =>
          Json.obj("orderId" -> orderId)
        }
        .map(Accepted(_))
    }
  }

  private def completedOrders(orderIds: Seq[OrderId]): Future[Seq[CompletedOrder]] = Future.sequence {
    orderIds.map { orderId =>
      val eventualCompletedOrder: Future[CompletedOrder] = for {
        orderSummary <- brokerService.getOrderSummary(orderId).invoke
      } yield {
        log.info(orderSummary.toString)
        toCompletedOrder(orderId, Some(orderSummary))
      }
      eventualCompletedOrder.recover {
        case ex: Throwable =>
          ex.printStackTrace()
          toCompletedOrder(orderId, None)
      }
    }
  }

  private def toCompletedOrder(orderId: OrderId, orderSummary: Option[OrderSummary]): CompletedOrder = {
    orderSummary match {
      case Some(orderSummary) =>
        CompletedOrder(
          orderId = orderId,
          symbol = Some(orderSummary.symbol),
          shares = Some(orderSummary.shares),
          price = orderSummary.status match {
            case fulfilled: OrderStatus.Fulfilled => Some(fulfilled.price)
            case _ => None
          },
          tradeType = Some(orderSummary.tradeType)
        )
      case None =>
        CompletedOrder(orderId)
    }
  }

}
