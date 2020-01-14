package stocktrader.wiretransfer.impl

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import stocktrader.wiretransfer.impl.transfer.{TransferCommand, TransferEvent, TransferState}

object WireTransferSerializerRegistry extends JsonSerializerRegistry {
  override def serializers = List(
    // State
    JsonSerializer[TransferState],
    // Commands
    JsonSerializer[TransferCommand.TransferFunds],
    JsonSerializer[TransferCommand.RequestFundsSuccessful.type],
    JsonSerializer[TransferCommand.RequestFundsFailed.type],
    JsonSerializer[TransferCommand.DeliveryFailed.type],
    JsonSerializer[TransferCommand.DeliverySuccessful.type],
    JsonSerializer[TransferCommand.RefundSuccessful.type],
    // Events
    JsonSerializer[TransferEvent.TransferInitiated],
    JsonSerializer[TransferEvent.FundsRetrieved],
    JsonSerializer[TransferEvent.CouldNotSecureFunds],
    JsonSerializer[TransferEvent.DeliveryConfirmed],
    JsonSerializer[TransferEvent.DeliveryFailed],
    JsonSerializer[TransferEvent.RefundDelivered],
  )
}
