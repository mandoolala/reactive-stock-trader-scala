package stocktrader.portfolio.api

import julienrf.json.derived
import play.api.libs.json._
import stocktrader.TransferId

sealed trait FundsTransfer

case class Deposit(transferId: TransferId, funds: BigDecimal) extends FundsTransfer

case class Withdrawal(transferId: TransferId, funds: BigDecimal) extends FundsTransfer

case class Refund(transferId: TransferId, funds: BigDecimal) extends FundsTransfer

object FundsTransfer {
  implicit val format: Format[FundsTransfer] = derived.flat.oformat((__ \ "type").format[String])
}
