package uk.gov.hmrc.timetopayproxy.models.saopled.ttpinform

import play.api.libs.json.{ JsObject, Json, OFormat }
import uk.gov.hmrc.timetopayproxy.models.error.{ InternalTtppError, TtppWriteableError }

final case class TtpInformGeneralFailureResponse(code: Int, details: String)
    extends InternalTtppError with TtppWriteableError {
  def toWriteableProxyError: TtppWriteableError = this
  def toJson: JsObject = Json.toJsObject(this)
}

object TtpInformGeneralFailureResponse {
  implicit val format: OFormat[TtpInformGeneralFailureResponse] = Json.format[TtpInformGeneralFailureResponse]
}
