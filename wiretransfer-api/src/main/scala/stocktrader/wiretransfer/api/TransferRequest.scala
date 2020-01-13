package stocktrader.wiretransfer.api

import julienrf.json.derived
import play.api.libs.json._
import stocktrader.TransferId

sealed trait TransferRequest {
  def account: Account
}

object TransferRequest {

  case class WithdrawalRequest(transferId: TransferId, account: Account, amount: BigDecimal) extends TransferRequest
  case class DepositRequest(transferId: TransferId, account: Account, amount: BigDecimal) extends TransferRequest

  implicit val format: Format[TransferRequest] = derived.flat.oformat((__ \ "type").format[String])

}
