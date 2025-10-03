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

package uk.gov.hmrc.timetopayproxy.models.saonly.ttpinform

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._
import play.api.libs.json.{JsNumber, JsResultException, JsString, Json}

class DdiReferenceSpec extends AnyFreeSpec {

  "DDIReference" - {
    "should encode and decode correctly" in {
      val ddiReferenceObject = DdiReference("TestDDI")
      val ddiReferenceJson = JsString("TestDDI")

      Json.toJson(ddiReferenceObject) shouldBe ddiReferenceJson
      ddiReferenceJson.as[DdiReference] shouldBe ddiReferenceObject
    }

    "should correctly fail to decode from invalid JSON" in {
      val badDdiReferenceJson = JsNumber(2)

      assertThrows[JsResultException](
        badDdiReferenceJson.as[DdiReference]
      )
    }
  }
}
