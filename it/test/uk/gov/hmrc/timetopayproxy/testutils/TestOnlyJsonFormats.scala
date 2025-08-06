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

package uk.gov.hmrc.timetopayproxy.testutils

import play.api.libs.json.{ Json, Writes }
import uk.gov.hmrc.timetopayproxy.models.{ TimeToPayEligibilityError, TimeToPayError, TimeToPayInnerError }

/** Provider of JSON writers and readers that have no use in production.
  * Kept here instead of in the main code so we can avoid the ambiguity that would arise from having unused formats.
  */
object TestOnlyJsonFormats {
  // Ensure these aren't full formats so they don't clash with the production writers/readers.

  implicit val testOnlyWritesTimeToPayError: Writes[TimeToPayError] = {
    implicit val testOnlyWritesTimeToPayInnerError: Writes[TimeToPayInnerError] =
      Json.writes[TimeToPayInnerError]

    Json.writes[TimeToPayError]
  }

  implicit val testOnlyWritesTimeToPayEligibilityError: Writes[TimeToPayEligibilityError] =
    Json.writes[TimeToPayEligibilityError]

}
