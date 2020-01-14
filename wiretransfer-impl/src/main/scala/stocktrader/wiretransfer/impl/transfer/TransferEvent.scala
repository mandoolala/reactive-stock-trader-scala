package stocktrader.wiretransfer.impl.transfer

import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, AggregateEventTagger}
import play.api.libs.json.Json
import stocktrader.TransferId

sealed trait TransferEvent extends AggregateEvent[TransferEvent] {
  def transferId: TransferId
  def transferDetails: TransferDetails
  override def aggregateTag: AggregateEventTagger[TransferEvent] = TransferEvent.Tag
}

object TransferEvent {

  val NumShards = 4
  val Tag = AggregateEventTag.sharded[TransferEvent](NumShards)

  //implicit val format: Format[TransferEvent] = derived.flat.oformat((__ \ "type").format[String])

  case class TransferInitiated(transferId: TransferId, transferDetails: TransferDetails) extends TransferEvent
  object TransferInitiated {
    implicit val format = Json.format[TransferInitiated]
  }

  case class FundsRetrieved(transferId: TransferId, transferDetails: TransferDetails) extends TransferEvent
  object FundsRetrieved {
    implicit val format = Json.format[FundsRetrieved]
  }

  case class CouldNotSecureFunds(transferId: TransferId, transferDetails: TransferDetails) extends TransferEvent
  object CouldNotSecureFunds {
    implicit val format = Json.format[CouldNotSecureFunds]
  }

  case class DeliveryConfirmed(transferId: TransferId, transferDetails: TransferDetails) extends TransferEvent
  object DeliveryConfirmed {
    implicit val format = Json.format[DeliveryConfirmed]
  }

  case class DeliveryFailed(transferId: TransferId, transferDetails: TransferDetails) extends TransferEvent
  object DeliveryFailed {
    implicit val format = Json.format[DeliveryFailed]
  }

  case class RefundDelivered(transferId: TransferId, transferDetails: TransferDetails) extends TransferEvent
  object RefundDelivered {
    implicit val format = Json.format[RefundDelivered]
  }

}