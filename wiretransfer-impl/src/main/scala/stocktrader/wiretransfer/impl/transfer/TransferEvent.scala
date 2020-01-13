package stocktrader.wiretransfer.impl

import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, AggregateEventTagger}
import julienrf.json.derived
import play.api.libs.json._
import stocktrader.TransferId

sealed trait TransferEvent extends AggregateEvent[TransferEvent] {
  def transferId: TransferId
  def transferDetails: TransferDetails
  override def aggregateTag: AggregateEventTagger[TransferEvent] = TransferEvent.Tag
}

object TransferEvent {

  implicit val format: Format[TransferEvent] = derived.flat.oformat((__ \ "type").format[String])

  val NumShards = 4
  val Tag = AggregateEventTag.sharded[TransferEvent](NumShards)

  case class TransferInitiated(transferId: TransferId, transferDetails: TransferDetails) extends TransferEvent

  case class FundsRetrieved(transferId: TransferId, transferDetails: TransferDetails) extends TransferEvent
  case class CouldNotSecureFunds(transferId: TransferId, transferDetails: TransferDetails) extends TransferEvent

  case class DeliveryConfirmed(transferId: TransferId, transferDetails: TransferDetails) extends TransferEvent
  case class DeliveryFailed(transferId: TransferId, transferDetails: TransferDetails) extends TransferEvent

  case class RefundDelivered(transferId: TransferId, transferDetails: TransferDetails) extends TransferEvent

}
