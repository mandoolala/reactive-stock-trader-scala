package stocktrader.wiretransfer.impl.transfer

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import julienrf.json.derived
import play.api.libs.json._
import stocktrader.wiretransfer.api.Account

sealed trait TransferCommand

object TransferCommand {

  implicit val format: Format[TransferCommand] = derived.flat.oformat((__ \ "type").format[String])

  case class TransferFunds(source: Account, destination: Account, amount: BigDecimal) extends TransferCommand with ReplyType[Done]

  //Command: RequestFunds
  //Event: FundsRequested, FundsSecured, FundsRequestFailed
  case object RequestFundsSuccessful extends TransferCommand with ReplyType[Done]
  case object RequestFundsFailed extends TransferCommand with ReplyType[Done]

  //command: sendFunds
  //Event: FundsSent, FundsDelivered, FundsDeliveryFailed
  case object DeliveryFailed extends TransferCommand with ReplyType[Done]
  case object DeliverySuccessful extends TransferCommand with ReplyType[Done]

  //command: RefundSender
  //Event: SenderRefunded, RefundFailed
  case object RefundSuccessful extends TransferCommand with ReplyType[Done]

}
