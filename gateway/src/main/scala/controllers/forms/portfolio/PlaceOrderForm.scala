package controllers.forms.portfolio

import controllers.forms.portfolio.PlaceOrderForm.Order
import controllers.forms.portfolio.PlaceOrderForm.Order.Order
import play.api.data.Forms.{mapping, _}
import play.api.data.format.Formatter
import play.api.data.{Form, _}
import stocktrader.TradeType.TradeType
import stocktrader.portfolio.api.order.{OrderDetails, OrderType}
import stocktrader.{JsonFormats, TradeType}

case class PlaceOrderForm(symbol: String, shares: Int, order: Order) {
  def toOrderDetails = OrderDetails(
    symbol = symbol,
    shares = shares,
    tradeType = Order.toTradeType(order),
    orderType = OrderType.Market
  )
}

object PlaceOrderForm {

  val form: Form[PlaceOrderForm] = Form(
    mapping(
      "symbol" -> text,
      "shares" -> number,
      "order" -> Forms.of[Order.Value]
    )(PlaceOrderForm.apply)(PlaceOrderForm.unapply)
  )

  object Order extends Enumeration {
    val buy, sell = Value
    type Order = Value
    implicit val jsonFormat = JsonFormats.enumFormat(Order)

    implicit def formFormat: Formatter[Order] = new Formatter[Order] {

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Order] = {
        data.get(key)
          .map(Order.withName)
          .toRight(Seq(FormError(key, "error.required", Nil)))
      }

      override def unbind(key: String, value: Order): Map[String, String] = {
        Map(key -> value.toString)
      }

    }

    def toTradeType(order: Order): TradeType = order match {
      case `buy` => TradeType.Buy
      case `sell` => TradeType.Sell
      case _ => throw new IllegalStateException()
    }

  }

}
