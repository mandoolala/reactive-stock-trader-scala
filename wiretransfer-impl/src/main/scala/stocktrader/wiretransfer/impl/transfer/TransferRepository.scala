package stocktrader.wiretransfer.impl.transfer

import akka.NotUsed
import akka.persistence.query.Offset
import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, EventStreamElement, PersistentEntityRef}
import stocktrader.TransferId

trait TransferRepository {
  def get(transferId: TransferId): PersistentEntityRef[TransferCommand]
  def eventStream(tag: AggregateEventTag[TransferEvent], offset: Offset): Source[EventStreamElement[TransferEvent], NotUsed]
}
