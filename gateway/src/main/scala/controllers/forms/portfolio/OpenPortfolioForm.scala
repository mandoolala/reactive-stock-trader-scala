package controllers.forms.portfolio

import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json
import stocktrader.portfolio.api.OpenPortfolioDetails

case class OpenPortfolioForm(name: String) {
  def toRequest = OpenPortfolioDetails(name)
}

object OpenPortfolioForm {

  val form: Form[OpenPortfolioForm] = Form(
    mapping(
      "name" -> text
    )(OpenPortfolioForm.apply)(OpenPortfolioForm.unapply)
  )

  implicit val format = Json.format[OpenPortfolioForm]

}
