package stocktrader.wiretransfer.api

import play.api.libs.json.{Format, Json}

case class Transfer(sourceAccount: Account, destinationAccount: Account, funds: BigDecimal)

object Transfer {
  implicit val format: Format[Transfer] = Json.format[Transfer]
}
