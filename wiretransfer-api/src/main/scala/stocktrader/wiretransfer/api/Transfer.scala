package stocktrader.wiretransfer.api

import play.api.libs.json.Json

case class Transfer(sourceAccount: Account, destinationAccount: Account, funds: BigDecimal)

object Transfer {
  implicit val format = Json.format[Transfer]
}
