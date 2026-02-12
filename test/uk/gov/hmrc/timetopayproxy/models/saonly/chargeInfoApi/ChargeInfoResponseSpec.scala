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

package uk.gov.hmrc.timetopayproxy.models.saonly.chargeInfoApi

import org.scalactic.source.Position
import org.scalamock.scalatest.MockFactory
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._
import play.api.libs.json._
import uk.gov.hmrc.timetopayproxy.config.FeatureSwitch
import uk.gov.hmrc.timetopayproxy.models.featureSwitches.SaRelease2Enabled
import uk.gov.hmrc.timetopayproxy.models.saonly.chargeInfoApi.ChargeInfoTestData.TestData
import uk.gov.hmrc.timetopayproxy.testutils.JsonAssertionOps.RichJsValueWithAssertions
import uk.gov.hmrc.timetopayproxy.testutils.schematestutils.Validators
import uk.gov.hmrc.timetopayproxy.testutils.schematestutils.impl.DebtTransSchemaValidator

class ChargeInfoResponseSpec extends AnyFreeSpec with MockFactory {

  "ChargeInfoResponse" - {
    "Release 1 schema" - {
      "implicit JSON writer (data going to our clients)" - {
        val timeToPayProxyR1Schema = Validators.TimeToPayProxy.ChargeInfo.Live.openApiResponseSuccessfulSchema

        "when all the optional fields are fully populated" - {
          val allOptionsJson: JsValue = TestData.WithOnlySomes.chargeInfoResponseR1JsonFromProxy
          val allOptionsObject: ChargeInfoResponse = TestData.WithOnlySomes.chargeInfoResponseR1

          testSchemaWriter(timeToPayProxyR1Schema, allOptionsJson, allOptionsObject)
        }

        "when only one optional field on each path is populated" - {
          val someOptionsJson: JsValue = TestData.With1SomeOnEachPath.chargeInfoResponseR1JsonFromProxy
          val someOptionsObject: ChargeInfoResponse = TestData.With1SomeOnEachPath.chargeInfoResponseR1

          testSchemaWriter(timeToPayProxyR1Schema, someOptionsJson, someOptionsObject)
        }

        "when none of the optional fields are populated" - {
          val noOptionsJson: JsValue = TestData.With0SomeOnEachPath.chargeInfoResponseR1JsonFromProxy
          val noOptionsObject: ChargeInfoResponse = TestData.With0SomeOnEachPath.chargeInfoResponseR1

          testSchemaWriter(timeToPayProxyR1Schema, noOptionsJson, noOptionsObject)
        }
      }

      "implicit JSON reader (data coming from time-to-pay-eligibility)" - {
        val timeToPayEligibilityR1Schema =
          Validators.TimeToPayEligibility.ChargeInfo.Live.openApiResponseSuccessfulSchema
        val saRelease2Disabled = SaRelease2Enabled(false)

        "when all the optional fields are fully populated" - {
          val allOptionsJson: JsValue = TestData.WithOnlySomes.chargeInfoResponseR1JsonFromEligibility
          val allOptionsObject: ChargeInfoResponse = TestData.WithOnlySomes.chargeInfoResponseR1

          testSchemaReader(timeToPayEligibilityR1Schema, allOptionsJson, allOptionsObject, saRelease2Disabled)
        }

        "when only one optional field on each path is populated" - {
          val someOptionsJson: JsValue = TestData.With1SomeOnEachPath.chargeInfoResponseR1JsonFromEligibility
          val someOptionsObject: ChargeInfoResponse = TestData.With1SomeOnEachPath.chargeInfoResponseR1

          testSchemaReader(timeToPayEligibilityR1Schema, someOptionsJson, someOptionsObject, saRelease2Disabled)
        }

        "when none of the optional fields are populated" - {
          val noOptionsJson: JsValue = TestData.With0SomeOnEachPath.chargeInfoResponseR1JsonFromEligibility
          val noOptionsObject: ChargeInfoResponse = TestData.With0SomeOnEachPath.chargeInfoResponseR1

          testSchemaReader(timeToPayEligibilityR1Schema, noOptionsJson, noOptionsObject, saRelease2Disabled)
        }
      }
    }

    "Release 2 schema" - {
      "implicit JSON writer (data going to our clients)" - {
        val timeToPayProxyRelease2Schema = Validators.TimeToPayProxy.ChargeInfo.Proposed.openApiResponseSuccessfulSchema

        "when all the optional fields are fully populated" - {
          val allOptionsJson: JsValue = TestData.WithOnlySomes.chargeInfoResponseR2JsonFromProxy
          val allOptionsObject: ChargeInfoResponse = TestData.WithOnlySomes.chargeInfoResponseR2

          testSchemaWriter(timeToPayProxyRelease2Schema, allOptionsJson, allOptionsObject)
        }

        "when only one optional field on each path is populated" - {
          val someOptionsJson: JsValue = TestData.With1SomeOnEachPath.chargeInfoResponseR2JsonFromProxy
          val someOptionsObject: ChargeInfoResponse = TestData.With1SomeOnEachPath.chargeInfoResponseR2

          testSchemaWriter(timeToPayProxyRelease2Schema, someOptionsJson, someOptionsObject)
        }

        "when none of the optional fields are populated" - {
          val noOptionsJson: JsValue = TestData.With0SomeOnEachPath.chargeInfoResponseR2JsonFromProxy
          val noOptionsObject: ChargeInfoResponse = TestData.With0SomeOnEachPath.chargeInfoResponseR2

          testSchemaWriter(timeToPayProxyRelease2Schema, noOptionsJson, noOptionsObject)
        }
      }

      "implicit JSON reader (data coming from time-to-pay-eligibility)" - {
        val timeToPayEligibilityR2Schema =
          Validators.TimeToPayEligibility.ChargeInfo.Proposed.openApiResponseSuccessfulSchema
        val saRelease2Enabled = SaRelease2Enabled(true)

        "when all the optional fields are fully populated" - {
          val allOptionsJson: JsValue = TestData.WithOnlySomes.chargeInfoResponseR2JsonFromEligibility
          val allOptionsObject: ChargeInfoResponse = TestData.WithOnlySomes.chargeInfoResponseR2

          testSchemaReader(timeToPayEligibilityR2Schema, allOptionsJson, allOptionsObject, saRelease2Enabled)
        }

        "when only one optional field on each path is populated" - {
          val someOptionsJson: JsValue = TestData.With1SomeOnEachPath.chargeInfoResponseR2JsonFromEligibility
          val someOptionsObject: ChargeInfoResponse = TestData.With1SomeOnEachPath.chargeInfoResponseR2

          testSchemaReader(timeToPayEligibilityR2Schema, someOptionsJson, someOptionsObject, saRelease2Enabled)
        }

        "when none of the optional fields are populated" - {
          val noOptionsJson: JsValue = TestData.With0SomeOnEachPath.chargeInfoResponseR2JsonFromEligibility
          val noOptionsObject: ChargeInfoResponse = TestData.With0SomeOnEachPath.chargeInfoResponseR2

          testSchemaReader(timeToPayEligibilityR2Schema, noOptionsJson, noOptionsObject, saRelease2Enabled)
        }
      }
    }
  }

  private def testSchemaWriter(
    schema: DebtTransSchemaValidator.OpenApi3DerivedSchema,
    json: JsValue,
    obj: ChargeInfoResponse
  )(implicit pos: Position): Unit = {
    def writerToClients: Writes[ChargeInfoResponse] = ChargeInfoResponse.writes

    "writes the correct JSON" in {
      writerToClients.writes(obj) shouldBeEquivalentTo json
    }

    "writes JSON compatible with our schema" in {
      val writtenJson: JsValue = writerToClients.writes(obj)

      schema.validateAndGetErrors(writtenJson) shouldBe Nil
    }
  }

  private def testSchemaReader(
    schema: DebtTransSchemaValidator.OpenApi3DerivedSchema,
    json: JsValue,
    obj: ChargeInfoResponse,
    saRelease2FeatureSwitchValue: SaRelease2Enabled
  )(implicit pos: Position): Unit = {
    "reads the JSON correctly" in {
      val mockFeatureSwitch: FeatureSwitch = mock[FeatureSwitch]
      (() => mockFeatureSwitch.saRelease2Enabled)
        .expects()
        .returning(saRelease2FeatureSwitchValue)
        .once()

      def readerFromTtpEligibility: Reads[ChargeInfoResponse] = ChargeInfoResponse.reads(mockFeatureSwitch)

      readerFromTtpEligibility.reads(json) shouldBe JsSuccess(obj)
    }

    "was tested against JSON compatible with the time-to-pay-eligibility schema" in {
      schema.validateAndGetErrors(json) shouldBe Nil
    }
  }
}
