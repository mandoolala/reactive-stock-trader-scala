package stocktrader.wiretransfer.impl.transfer

import akka.NotUsed
import akka.persistence.query.Offset
import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, EventStreamElement, PersistentEntityRef, PersistentEntityRegistry}
import stocktrader.TransferId

class TransferRepositoryImpl(entityRegistry: PersistentEntityRegistry) extends TransferRepository {

  // TODO: Register TransferEntity

  override def get(transferId: TransferId): PersistentEntityRef[TransferCommand] = {
    entityRegistry.refFor[TransferEntity](transferId)
  }

  override def eventStream(tag: AggregateEventTag[TransferEvent], offset: Offset): Source[EventStreamElement[TransferEvent], NotUsed] = {
    entityRegistry.eventStream(tag, offset)
  }

}
