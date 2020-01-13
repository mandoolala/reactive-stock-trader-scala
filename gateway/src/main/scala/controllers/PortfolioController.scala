package controllers

import javax.inject._
import play.api.mvc._

@Singleton
class PortfolioController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def getAllPortfolios() = Action { _ =>
    Ok("TODO")
  }

  def openPortfolio() = Action { _ =>
    Ok("TODO")
  }

  def placeOrder(portfolioId: String) = Action { _ =>
    Ok("TODO")
  }

  def getPortfolio(portfolioId: String) = Action { _ =>
    Ok("TODO")
  }

}
