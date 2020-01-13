package stocktrader

import play.api.libs.json._

object JsonFormats {

  def enumReads[E <: Enumeration](enum: E): Reads[E#Value] = Reads {
    case JsString(s) =>
      try {
        JsSuccess(enum.withName(s).asInstanceOf[E#Value])
      } catch {
        case _: NoSuchElementException =>
          JsError(s"Enumeration expected of type: '${enum.getClass}', but it does not contain '$s'")
      }
    case _ => JsError("String value expected")
  }

  def enumWrites[E <: Enumeration]: Writes[E#Value] = Writes(v => JsString(v.toString))

  def enumFormat[E <: Enumeration](enum: E): Format[E#Value] = {
    Format(enumReads(enum), enumWrites)
  }

}
