package uk.gov.hmrc.timetopayproxy.models.saopled.ttpinform

import play.api.libs.json.{Json, OFormat}

final case class DdiReference(value: String)

object DdiReference {
  implicit val format: OFormat[DdiReference] = Json.format[DdiReference]
}
