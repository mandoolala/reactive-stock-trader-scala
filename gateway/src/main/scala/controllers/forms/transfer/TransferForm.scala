package controllers.forms.transfer

import controllers.forms.transfer.TransferForm.AccountType.AccountType
import play.api.data.{Form, Forms}
import play.api.data.Forms.mapping
import stocktrader.{AccountId, JsonFormats}
import play.api.data.Forms.{mapping, _}
import play.api.data.format.Formatter
import play.api.data.{Form, _}

case class TransferForm(amount: BigDecimal,
                        sourceType: AccountType,
                        sourceId: AccountId,
                        destinationType: AccountType,
                        destinationId: AccountId)

object TransferForm {

  val form: Form[TransferForm] = Form(
    mapping(
      "amount" -> bigDecimal,
      "sourceType" -> Forms.of[AccountType.Value],
      "sourceId" -> text,
      "destinationType" -> Forms.of[AccountType.Value],
      "destinationId" -> text
    )(TransferForm.apply)(TransferForm.unapply)
  )

  object AccountType extends Enumeration {
    val portfolio, savings = Value
    type AccountType = Value
    implicit val jsonFormat = JsonFormats.enumFormat(AccountType)

    implicit def formFormat: Formatter[AccountType] = new Formatter[AccountType] {

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], AccountType] = {
        data.get(key)
          .map(AccountType.withName)
          .toRight(Seq(FormError(key, "error.required", Nil)))
      }

      override def unbind(key: String, value: AccountType): Map[String, String] = {
        Map(key -> value.toString)
      }

    }
  }

}
