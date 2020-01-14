package stocktrader.broker.impl.quote

import akka.actor.ActorSystem
import akka.pattern.CircuitBreaker
import com.typesafe.config.Config
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import stocktrader.broker.api.Quote

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class IexQuoteServiceImpl(wsClient: WSClient, config: Config, actorSystem: ActorSystem)(implicit ec: ExecutionContext) extends QuoteService {

  private val hostname = config.getString("quote.iex.hostname")
  private val requestTimeout = 1000.millis
  private val callTimeout = requestTimeout.minus(requestTimeout.div(10))
  private val resetTimeout = 1000.millis
  private val circuitBreaker = new CircuitBreaker(
    executor = actorSystem.dispatcher,
    scheduler = actorSystem.scheduler,
    maxFailures = 10,
    callTimeout = callTimeout,
    resetTimeout = resetTimeout
  )

  override def getQuote(symbol: String): Future[Quote] = {
    val request: Future[WSResponse] = circuitBreaker.withCircuitBreaker(
      quoteRequest(symbol).withRequestTimeout(requestTimeout).get()
    )
    request.map { response =>
      val iexQuoteResponse = response.json.as[IexQuoteResponse]
      Quote(symbol, iexQuoteResponse.latestPrice)
    }
  }

  private def quoteRequest(symbol: String): WSRequest = {
    val url = s"https://$hostname/1.0/stock/$symbol/quote"
    wsClient.url(url)
  }

}
