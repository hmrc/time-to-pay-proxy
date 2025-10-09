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

package uk.gov.hmrc.timetopayproxy.config

import com.typesafe.config.ConfigFactory
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.Configuration
import uk.gov.hmrc.timetopayproxy.models.featureSwitches.InternalAuthEnabled

class FeatureSwitchSpec extends AnyFreeSpec with Matchers {
  val config: Configuration = Configuration(ConfigFactory.load())
  val maybeFeatureSwitchConfig: Option[Configuration] = config.getOptional[Configuration](s"feature-switch")
  val featureSwitch: FeatureSwitch = FeatureSwitch(maybeFeatureSwitchConfig)

  "FeatureSwitch" - {
    "internalAuthEnabled" - {
      "should retrieve the correct value from application.conf" in {
        featureSwitch.internalAuthEnabled shouldBe InternalAuthEnabled(true)
      }
    }

    "fullAmendEndpointEnabled" - {
      "should retrieve the correct value from application.conf" in {
        featureSwitch.fullAmendEndpointEnabled shouldBe true
      }
    }

    "cancelEndpointEnabled" - {
      "should retrieve the correct value from application.conf" in {
        featureSwitch.cancelEndpointEnabled shouldBe true
      }
    }

    "chargeInfoEndpointEnabled" - {
      "should retrieve the correct value from application.conf" in {
        featureSwitch.chargeInfoEndpointEnabled shouldBe true
      }
    }

    "informEndpointEnabled" - {
      "should retrieve the correct value from application.conf" in {
        featureSwitch.informEndpointEnabled shouldBe true
      }
    }
  }
}
