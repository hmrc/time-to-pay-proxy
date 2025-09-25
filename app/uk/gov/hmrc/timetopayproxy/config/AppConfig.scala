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

package uk.gov.hmrc.timetopayproxy.config

import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.timetopayproxy.models.featureSwitches.InternalAuthEnabled

import javax.inject.{ Inject, Singleton }

@Singleton
class AppConfig @Inject() (
  config: Configuration,
  servicesConfig: ServicesConfig
) {

  val authBaseUrl: String = servicesConfig.baseUrl("auth")
  val ttpBaseUrl: String = servicesConfig.baseUrl("ttp")
  val ttpeBaseUrl: String = servicesConfig.baseUrl("ttpe")
  val stubBaseUrl: String = servicesConfig.baseUrl("stub")
  val ttpToken: String = config.get[String]("microservice.services.ttp.token")
  // TODO DTD-2356: microservice.services.ttp.useIf config value may not be needed for TTP as TTP will always be on MDTP. We don't need to go via IF
  val useIf: Boolean = config.get[Boolean]("microservice.services.ttp.useIf")
  val auditingEnabled: Boolean = config.get[Boolean]("auditing.enabled")
  val graphiteHost: String = config.get[String]("microservice.metrics.graphite.host")
  val featureSwitch: Option[Configuration] = config.getOptional[Configuration](s"feature-switch")

  val internalAuthToken: String = config.get[String]("internal-auth.token")

  def internalAuthEnabled: InternalAuthEnabled =
    InternalAuthEnabled(
      featureSwitch
        .flatMap(_.getOptional[Boolean]("internalAuthEnabled"))
        .getOrElse(false)
    )
}
