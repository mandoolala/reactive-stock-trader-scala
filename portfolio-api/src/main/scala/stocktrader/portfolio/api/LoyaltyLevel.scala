package stocktrader.portfolio.api

import play.api.libs.json.Format
import stocktrader.JsonFormats
import stocktrader.portfolio.api

object LoyaltyLevel extends Enumeration {

  val Bronze, Silver, Gold = Value

  type LoyaltyLevel = Value

  implicit val format: Format[api.LoyaltyLevel.Value] = JsonFormats.enumFormat(LoyaltyLevel)

}
