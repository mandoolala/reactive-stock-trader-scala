package stocktrader.wiretransfer.api

import play.api.libs.json.{Format, Json}

case class TransactionSummary(id: String,
                              status: String,
                              dateTime: String,
                              source: String,
                              destination: String,
                              amount: String)

object TransactionSummary {
  implicit val format: Format[TransactionSummary] = Json.format[TransactionSummary]
}
