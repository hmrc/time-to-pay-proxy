/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.timetopayproxy.models.currency

import play.api.libs.json.{ Format, JsError, JsNumber, JsResult, JsSuccess, JsValue }

sealed abstract case class GbpPounds(value: BigDecimal) {
  // BigDecimal is based on a private BigInteger and the scale is the number of digits dedicated to the fractional part.
  // This is a last-ditch guarantee that the stored value is valid. This file should not allow this requirement to fail.
  require(value.scale == 2, s"Implementation error: GbpPounds guarantees a scale of 2; found ${value.scale} for $value")
}

object GbpPounds {
  def apply(value: BigDecimal): Either[String, GbpPounds] =
    if ((value * 100).isWhole) {
      Right(new GbpPounds(value.setScale(2)) {})
    } else
      Left(s"Number of digits after decimal point should not exceed 2: $value")

  def createOrThrow(value: BigDecimal): GbpPounds =
    GbpPounds(value) match {
      case Right(gbpPounds)   => gbpPounds
      case Left(errorMessage) => throw new IllegalArgumentException(errorMessage)
    }

  implicit val format: Format[GbpPounds] = new Format[GbpPounds] {
    def reads(json: JsValue): JsResult[GbpPounds] =
      json.validate[BigDecimal] match {
        case JsSuccess(value, _) =>
          GbpPounds(value) match {
            case Right(gbpPounds)   => JsSuccess(gbpPounds)
            case Left(errorMessage) => JsError(errorMessage)
          }
        case jsError: JsError => jsError
      }

    def writes(o: GbpPounds): JsValue = JsNumber(o.value)
  }
}
