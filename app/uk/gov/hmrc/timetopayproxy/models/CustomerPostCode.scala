/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.timetopayproxy.models
import java.time.LocalDate

import play.api.libs.json.Json

final case class PostCode(value: String) extends AnyVal

object PostCode extends ValueTypeFormatter {
  implicit val format =
    valueTypeFormatter(PostCode.apply, PostCode.unapply)
}

final case class CustomerPostCode(
    addressPostcode: PostCode,
    postcodeDate: LocalDate
) {
  require(
    !addressPostcode.value.trim().isEmpty(),
    "addressPostcode should not be empty"
  )
}

object CustomerPostCode {
  implicit val format = Json.format[CustomerPostCode]
}
