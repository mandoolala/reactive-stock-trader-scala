package stocktrader.wiretransfer.api

import julienrf.json.derived
import play.api.libs.json._
import stocktrader.{AccountId, PortfolioId}

sealed trait Account

object Account {

  case class Portfolio(portfolioId: PortfolioId) extends Account
  case class SavingsAccount(accountId: AccountId) extends Account

  implicit val format: Format[Account] = derived.flat.oformat((__ \ "type").format[String])

}
