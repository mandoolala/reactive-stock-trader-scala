package stocktrader.portfolio.impl

import play.api.libs.json.Json
import stocktrader.portfolio.api.Holding

case class Holdings(holdings: Map[String, Int]){
  def add(symbol: String, newShares: Int): Holdings = {
    val currentShares = holdings.getOrElse(symbol, 0)
    Holdings(holdings + (symbol -> (currentShares + newShares)))
  }

  def remove(symbol: String, sharesToRemove: Int): Holdings = {
    require(sharesToRemove > 0, "Number of shares to remove from Holdings must be positive.")
    val currentShares = holdings.getOrElse(
      key = symbol,
      default = throw new IllegalStateException(s"Attempt to remove shares for symbol $symbol not contained in Holdings.")
    )
    val remainingShares = currentShares - sharesToRemove
    require(remainingShares >= 0, "Attempt to remove more shares from Holdings than are currently available.")
    if (remainingShares > 0) Holdings(holdings + (symbol -> remainingShares))
    else Holdings(holdings - symbol)
  }

  def asSequence: Seq[Holding] = holdings.toList.map {
    case(symbol, shareCount) => Holding(symbol, shareCount)
  }

  def getShareCount(symbol: String): Int = holdings.getOrElse(symbol, 0)

}

object Holdings {
  def empty = Holdings(Map.empty)

  implicit val format = Json.format[Holdings]
}