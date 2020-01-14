package models

import play.api.libs.json.Json

import stocktrader.OrderId
import stocktrader.TradeType.TradeType

case class CompletedOrder(orderId: OrderId,
                         symbol: Option[String] = None,
                         shares: Option[Int] = None,
                         price: Option[BigDecimal] = None,
                         tradeType: Option[TradeType] = None)
object CompletedOrder {
  implicit val format = Json.format[CompletedOrder]
}