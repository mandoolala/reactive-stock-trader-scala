package stocktrader.wiretransfer.impl

import play.api.libs.json.Format
import stocktrader.JsonFormats
import stocktrader.wiretransfer.impl.TransferState.Status.Status

case class TransferState(transferDetails: TransferDetails, status: Status)

object TransferState {

  def from(transferDetails: TransferDetails): TransferState = TransferState(transferDetails, Status.FundsRequested)

  object Status extends Enumeration {

    val FundsRequested, UnableToSecureFunds, FundsSent, DeliveryConfirmed, RefundSent, RefundDelivered = Value

    type Status = Value

    implicit val format: Format[TransferState.Status.Value] = JsonFormats.enumFormat(Status)

  }

}
