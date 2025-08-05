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

package uk.gov.hmrc.timetopayproxy.utils.json

import cats.data.NonEmptyList
import play.api.libs.json.{ Format, JsonValidationError, Reads, Writes }

object CatsNonEmptyListJson {
  // These values are not implicit because IntelliJ reports them as unused in Play's JSON macros.
  // Using them as implicit imports would cause IntelliJ's "optimize imports" to remove them.

  def nonEmptyListWriter[Element](implicit writer: Writes[Element]): Writes[NonEmptyList[Element]] =
    Writes.list[Element].contramap(_.toList)

  def nonEmptyListReader[Element](implicit reader: Reads[Element]): Reads[NonEmptyList[Element]] =
    Reads
      .list[Element]
      .map(NonEmptyList.fromList)
      .collect(JsonValidationError("NonEmptyList in JSON reader was empty")) { case Some(result) => result }

  def nonEmptyListFormat[Element](implicit format: Format[Element]): Format[NonEmptyList[Element]] =
    Format(nonEmptyListReader, nonEmptyListWriter)
}
