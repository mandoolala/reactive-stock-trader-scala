package stocktrader.portfolio.impl

import akka.stream.scaladsl.Flow
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer

import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import org.slf4j.{Logger, LoggerFactory}
import stocktrader.broker.api.{BrokerService, OrderResult}
import stocktrader.portfolio.api._
import stocktrader.portfolio.api.order.OrderDetails
import stocktrader.{OrderId, PortfolioId}

import scala.concurrent.{ExecutionContext, Future}

class PortfolioServiceImpl(portfolioRepository: PortfolioRepository,
                           brokerService: BrokerService,
                           db: CassandraSession)(implicit ec: ExecutionContext) extends PortfolioService {

  private final val log: Logger = LoggerFactory.getLogger(classOf[PortfolioServiceImpl])

  brokerService.orderResult().subscribe.atLeastOnce(Flow[OrderResult].mapAsync(1)(handleOrderResult))

  override def openPortfolio(): ServiceCall[OpenPortfolioDetails, PortfolioId] = portfolioRepository.open

  override def closePortfolio(portfolioId: PortfolioId): ServiceCall[NotUsed, Done] = { _ =>
    portfolioRepository.getRef(portfolioId).ask(PortfolioCommand.ClosePortfolio)
  }

  override def getPortfolio(portfolioId: PortfolioId): ServiceCall[NotUsed, PortfolioView] = { _ =>
    portfolioRepository.get(portfolioId).view()
  }

  override def getAllPortfolios(): ServiceCall[NotUsed, Seq[PortfolioSummary]] = _ =>
    db.selectAll("SELECT portfolioId, name FROM portfolio_summary;").map { rows =>
      rows.map { row =>
        PortfolioSummary(
          portfolioId = row.getString("portfolioId"),
          name = row.getString("name")
        )
      }
    }

  override def placeOrder(portfolioId: PortfolioId): ServiceCall[OrderDetails, OrderId] = { orderDetails =>
    val orderId = OrderId.newId
    portfolioRepository.get(portfolioId)
      .placeOrder(orderId, orderDetails)
      .map(_ => orderId)
  }

  override def processTransfer(portfolioId: PortfolioId): ServiceCall[FundsTransfer, Done] = { fundsTransfer =>
    val portfolioRef = portfolioRepository.getRef(portfolioId)
    fundsTransfer match {
      case deposit: FundsTransfer.Deposit => portfolioRef.ask(PortfolioCommand.ReceiveFunds(deposit.funds))
      case withdrawal: FundsTransfer.Withdrawal => portfolioRef.ask(PortfolioCommand.SendFunds(withdrawal.funds))
      case refund: FundsTransfer.Refund => portfolioRef.ask(PortfolioCommand.AcceptRefund(refund.funds, refund.transferId))
    }
  }

  override def orderPlaced(): Topic[OrderPlaced] = {
    TopicProducer.taggedStreamWithOffset(PortfolioEvent.Tag.allTags.toList) { (tag, offset) =>
      portfolioRepository.ordersStream(tag, offset)
    }
  }

  private def handleOrderResult(orderResult: OrderResult): Future[Done] = {
    val portfolio = portfolioRepository.get(orderResult.portfolioId)
    orderResult match {
      case orderFulfilled: OrderResult.Fulfilled =>
        portfolio.processTrade(orderFulfilled.orderId, orderFulfilled.trade)
      case orderFailed: OrderResult.Failed =>
        portfolio.orderFailed(orderFailed)
    }
  }


}
