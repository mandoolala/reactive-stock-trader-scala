package stocktrader

object TradeType extends Enumeration {
  val Buy, Sell = Value
  type TradeType = Value
  implicit val format = JsonFormats.enumFormat(TradeType)
}