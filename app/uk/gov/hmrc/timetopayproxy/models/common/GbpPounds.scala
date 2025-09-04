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

package uk.gov.hmrc.timetopayproxy.models.common

import play.api.libs.json._

import scala.util.Try

final case class GbpPounds(value: BigDecimal) extends AnyVal

object GbpPounds {

  def createOrThrow(amount: BigDecimal): GbpPounds =
    if (amount.scale <= 2 && amount >= 0) {
      GbpPounds(amount.setScale(2))
    } else {
      throw new IllegalArgumentException(s"Invalid GBP amount: $amount")
    }

  implicit val format: Format[GbpPounds] = new Format[GbpPounds] {
    def reads(json: JsValue): JsResult[GbpPounds] = json match {
      case JsNumber(value) =>
        Try {
          if (value.scale <= 2 && value >= 0) {
            JsSuccess(GbpPounds(value.setScale(2)))
          } else {
            JsError("Invalid GBP amount - must be non-negative with at most 2 decimal places")
          }
        }.getOrElse(JsError("Invalid number format"))
      case _ => JsError("Expected number")
    }

    def writes(gbp: GbpPounds): JsValue = JsNumber(gbp.value)
  }
}
