package stocktrader.broker.api

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import stocktrader.OrderId

trait BrokerService extends Service {

  import Service._

  def getQuote(symbol: String): ServiceCall[NotUsed, Quote]
  def getOrderSummary(orderId: OrderId): ServiceCall[NotUsed, OrderSummary]

  private val OrderResultsTopicId = "Broker-OrderResults"
  def orderResult(): Topic[OrderResult]

  override def descriptor: Descriptor = {
    named("broker-service")
      .withCalls(
        restCall(Method.GET, "/api/quote/:symbol", getQuote _),
        restCall(Method.GET, "/api/order/:orderId", getOrderSummary _)
      )
      .withTopics(
        topic(OrderResultsTopicId, orderResult _)
          .addProperty(
            KafkaProperties.partitionKeyStrategy,
            PartitionKeyStrategy[OrderResult](_.portfolioId)
          )
      )
  }
}
