package uk.gov.hmrc.timetopayproxy.models.saopled.ttpinform

import play.api.libs.json.{JsObject, Json, OFormat}
import uk.gov.hmrc.timetopayproxy.models.error.{InternalTtppError, TtppWriteableError}
import uk.gov.hmrc.timetopayproxy.models.saopled.common.ProcessingDateTimeInstant
import uk.gov.hmrc.timetopayproxy.models.saopled.common.apistatus.ApiStatus

/** This is intended for both `200 OK` and `500 Internal Server Error`.
 * This is also an incoming error from the `time-to-pay` service.
 */

sealed abstract class TtpInformInformativeResponse(
  apisCalled: List[ApiStatus],
  processingDateTime: ProcessingDateTimeInstant
)

final case class TtpInformInformativeSuccess(
  apisCalled: List[ApiStatus],
  processingDateTime: ProcessingDateTimeInstant
) extends TtpInformInformativeResponse(apisCalled = apisCalled, processingDateTime = processingDateTime)

object TtpInformInformativeSuccess {
  implicit val format: OFormat[TtpInformInformativeSuccess] = Json.format[TtpInformInformativeSuccess]
}

final case class TtpInformInformativeError(
  apisCalled: List[ApiStatus],
  processingDateTime: ProcessingDateTimeInstant
) extends TtpInformInformativeResponse(apisCalled = apisCalled, processingDateTime = processingDateTime) with InternalTtppError with TtppWriteableError {
    def toWriteableProxyError: TtppWriteableError = this
    def toJson: JsObject = Json.toJsObject(this)
}

object TtpInformInformativeError {
  implicit val format: OFormat[TtpInformInformativeError] = Json.format[TtpInformInformativeError]
}