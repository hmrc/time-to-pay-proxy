/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.timetopayproxy.models.saonly.chargeInfoApi

import play.api.libs.json.{ Format, Json, OFormat }

final case class Signal(signalType: SignalType, signalValue: SignalValue, signalDescription: Option[String])

object Signal {
  implicit val format: OFormat[Signal] = Json.format[Signal]
}

final case class SignalType(entryName: String) extends AnyVal

object SignalType {
  implicit val format: Format[SignalType] = Json.valueFormat[SignalType]
}

final case class SignalValue(value: String) extends AnyVal

object SignalValue {
  implicit val format: Format[SignalValue] = Json.valueFormat[SignalValue]
}
