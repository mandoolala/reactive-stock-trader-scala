package controllers

import akka.stream.scaladsl.{Flow, Sink}
import controllers.forms.transfer.TransferForm
import controllers.forms.transfer.TransferForm.AccountType
import controllers.forms.transfer.TransferForm.AccountType.AccountType
import org.slf4j.{Logger, LoggerFactory}
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents, WebSocket}
import stocktrader.wiretransfer.api.{Account, TransactionSummary, Transfer, WireTransferService}
import stocktrader.{AccountId, PortfolioId}

import scala.concurrent.{ExecutionContext, Future}

class WireTransferController(cc: ControllerComponents,
                             wireTransferService: WireTransferService)(implicit ec: ExecutionContext)
  extends AbstractController(cc) with I18nSupport {

  private final val log: Logger = LoggerFactory.getLogger(classOf[WireTransferController])

  def transfer() = Action.async { implicit request =>
    val form = TransferForm.form.bindFromRequest
    if (form.hasErrors) {
      Future.successful(BadRequest(form.errorsAsJson))
    } else {
      val transfer = populateTransfer(form.get)
      wireTransferService.transferFunds().invoke(transfer)
        .map { transferId =>
          Json.obj("transferId" -> transferId)
        }
        .map(Accepted(_))
    }
  }

  def getAllTransfersFor(portfolioId: PortfolioId) = Action.async { _ =>
    wireTransferService.getAllTransactionsFor(portfolioId).invoke
      .map(Json.toJson[Seq[TransactionSummary]](_))
      .map(Ok(_))
  }

  def ws(): WebSocket = {
    WebSocket.acceptOrResult { _ =>
      wireTransferService.transferStream().invoke
        .map { source =>
          Right(Flow.fromSinkAndSource(Sink.ignore, source))
        }
    }
  }

  private def populateTransfer(transferForm: TransferForm): Transfer = {
    val sourceAccount = getAccount(transferForm.sourceType, transferForm.sourceId)
    val destinationAccount = getAccount(transferForm.destinationType, transferForm.destinationId)
    Transfer(sourceAccount, destinationAccount, transferForm.amount)
  }

  private def getAccount(accountType: AccountType, accountId: AccountId): Account = {
    accountType match {
      case AccountType.portfolio =>
        Account.Portfolio(accountId) // FIXME: AccountId = PortfolioId ?
      case AccountType.savings =>
        Account.SavingsAccount(accountId)
      case _ =>
        log.error(accountType.toString)
        throw new IllegalStateException()
    }
    ???
  }

}
