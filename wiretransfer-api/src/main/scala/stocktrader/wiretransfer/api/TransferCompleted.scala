package stocktrader.wiretransfer.api

import play.api.libs.json.{Format, Json}

case class TransferCompleted(id: String,
                             status: String,
                             dateTime: String,
                             sourceType: String,
                             sourceId: String,
                             destinationType: String,
                             destinationId: String,
                             amount: String)

object TransferCompleted {
  implicit val format: Format[TransferCompleted] = Json.format[TransferCompleted]
}
