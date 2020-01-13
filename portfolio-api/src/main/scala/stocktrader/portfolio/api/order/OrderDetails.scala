package stocktrader.portfolio.api.order

import play.api.libs.json.{Format, Json}
import stocktrader.TradeType._

case class OrderDetails(symbol: String,
                        shares: Int,
                        tradeType: TradeType,
                        orderType: OrderType)

object OrderDetails {
  implicit val format: Format[OrderDetails] = Json.format
}
