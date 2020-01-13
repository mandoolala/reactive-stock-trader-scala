package stocktrader.wiretransfer.impl.transfer

import stocktrader.JsonFormats
import stocktrader.wiretransfer.impl.transfer.TransferState.Status.Status

case class TransferState(transferDetails: TransferDetails, status: Status)

object TransferState {

  object Status extends Enumeration {

    val FundsRequested, UnableToSecureFunds, FundsSent, DeliveryConfirmed, RefundSent, RefundDelivered = Value

    type Status = Value
    implicit val format = JsonFormats.enumFormat(Status)

  }

}
