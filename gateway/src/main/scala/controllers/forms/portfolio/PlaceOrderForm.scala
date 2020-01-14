package controllers.forms.portfolio

import controllers.forms.portfolio.PlaceOrderForm.Order.Order
import stocktrader.{JsonFormats, TradeType}
import stocktrader.TradeType.TradeType

case class PlaceOrderForm(symbol: String, shares: Int, order: Order)

object PlaceOrderForm {

  object Order extends Enumeration {
    val buy, sell = Value
    type Order = Value
    implicit val format = JsonFormats.enumFormat(Order)

    def toTradeType: TradeType = this match {
      case `buy` => TradeType.Buy
      case `sell` => TradeType.Sell
      case _ => throw new IllegalStateException()
    }

  }

}
