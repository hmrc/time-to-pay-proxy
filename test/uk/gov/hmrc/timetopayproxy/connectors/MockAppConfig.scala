/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.timetopayproxy.connectors

import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.timetopayproxy.config.AppConfig

class MockAppConfig(config: Configuration, servicesConfig: ServicesConfig, ifImpl: Boolean)
    extends AppConfig(config, servicesConfig) {
  override val authBaseUrl: String = "http://localhost:11111"
  override val ttpBaseUrl: String = "http://localhost:11111"
  override val ttpeBaseUrl: String = "http://localhost:11111"
  override val ttpToken: String = "Token"
  override val useIf: Boolean = ifImpl
  override val auditingEnabled: Boolean = false
  override val graphiteHost: String = "http://localhost:11111"
}
