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
import uk.gov.hmrc.timetopayproxy.models.featureSwitches.{ EnrolmentAuthEnabled, InternalAuthEnabled }

case class FeatureSwitch(value: Option[Configuration]) {
  // Implement feature switch getter methods here.
  def cancelEndpointEnabled: Boolean = value.exists(_.get[Boolean]("endpoint.cancel.enabled"))
  def chargeInfoEndpointEnabled: Boolean = value.exists(_.get[Boolean]("endpoint.charge-info.enabled"))
  def fullAmendEndpointEnabled: Boolean = value.exists(_.get[Boolean]("endpoint.full-amend.enabled"))
  def informEndpointEnabled: Boolean = value.exists(_.get[Boolean]("endpoint.inform.enabled"))
  def internalAuthEnabled: InternalAuthEnabled = InternalAuthEnabled(
    value.exists(_.get[Boolean]("internalAuthEnabled"))
  )
  def enrolmentAuthEnabled: EnrolmentAuthEnabled = EnrolmentAuthEnabled(
    value.exists(_.get[Boolean]("enrolmentAuthEnabled"))
  )
}
