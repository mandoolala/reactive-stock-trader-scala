package stocktrader.broker.api

import julienrf.json.derived
import play.api.libs.json._
import stocktrader.{OrderId, PortfolioId}

sealed trait OrderResult {
  def portfolioId: PortfolioId
  def orderId: OrderId
}

object OrderResult {

  case class Fulfilled(portfolioId: PortfolioId, orderId: OrderId, trade: Trade) extends OrderResult
  case class Failed(portfolioId: PortfolioId, orderId: OrderId) extends OrderResult

  implicit val format: Format[OrderResult] = derived.flat.oformat((__ \ "type").format[String])

}