package stocktrader.wiretransfer.api

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import stocktrader.{PortfolioId, TransferId}

trait WireTransferService extends Service {

  def transferFunds(): ServiceCall[Transfer, TransferId]
  def transferStream(): ServiceCall[NotUsed, Source[String, NotUsed]]
  def getAllTransactionsFor(portfolioId: PortfolioId): ServiceCall[NotUsed, Seq[TransactionSummary]]

  final val TransferRequestTopicId = "transfers"
  def transferRequest(): Topic[TransferRequest]

  override def descriptor: Descriptor = {
    import Service._
    named("wiretransfer-service")
      .withCalls(
        call(transferFunds _),
        call(transferStream _),
        restCall(Method.GET, "/api/transfer/:portfolioId", getAllTransactionsFor _)
      )
      .withTopics(
        topic(TransferRequestTopicId, transferRequest _)
      )
      .withAutoAcl(true)
  }
}
