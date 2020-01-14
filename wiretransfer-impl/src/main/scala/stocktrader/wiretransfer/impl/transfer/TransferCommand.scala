package stocktrader.wiretransfer.impl.transfer

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.playjson.JsonSerializer
import play.api.libs.json.Json
import stocktrader.wiretransfer.api.Account

sealed trait TransferCommand

object TransferCommand {

  case class TransferFunds(source: Account, destination: Account, amount: BigDecimal) extends TransferCommand with ReplyType[Done]

  object TransferFunds {
    implicit val format = Json.format[TransferFunds]
    //implicit val format: Format[TransferCommand] = derived.flat.oformat((__ \ "type").format[String])
  }

  //Command: RequestFunds
  //Event: FundsRequested, FundsSecured, FundsRequestFailed
  case object RequestFundsSuccessful extends TransferCommand with ReplyType[Done] {
    implicit val format = JsonSerializer.emptySingletonFormat(RequestFundsSuccessful)
  }
  case object RequestFundsFailed extends TransferCommand with ReplyType[Done] {
    implicit val format = JsonSerializer.emptySingletonFormat(RequestFundsFailed)
  }

  //command: sendFunds
  //Event: FundsSent, FundsDelivered, FundsDeliveryFailed
  case object DeliveryFailed extends TransferCommand with ReplyType[Done] {
    implicit val format = JsonSerializer.emptySingletonFormat(DeliveryFailed)
  }
  case object DeliverySuccessful extends TransferCommand with ReplyType[Done] {
    implicit val format = JsonSerializer.emptySingletonFormat(DeliverySuccessful)
  }

  //command: RefundSender
  //Event: SenderRefunded, RefundFailed
  case object RefundSuccessful extends TransferCommand with ReplyType[Done] {
    implicit val format = JsonSerializer.emptySingletonFormat(RefundSuccessful)
  }

}