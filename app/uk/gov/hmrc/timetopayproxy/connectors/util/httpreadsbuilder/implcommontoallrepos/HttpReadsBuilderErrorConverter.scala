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

package uk.gov.hmrc.timetopayproxy.connectors.util.httpreadsbuilder.implcommontoallrepos

/** Instances can convert given `HttpReadsBuilderError` into something connectors might understand. */
/* ℹ️ Note about updating this file: ℹ️
 * This file must be kept consistent with every copy in the other debt-transformation repos.
 * The most complex features are required by time-to-pay, so that is the best place to test any refactoring.
 *
 * Not sealed because tests might want to do something simpler so they don't have to deal with the repo-specific errors.
 */
private[httpreadsbuilder] trait HttpReadsBuilderErrorConverter[+InternalConnectorErrors] {
  def toConnectorError[ServiceError >: InternalConnectorErrors](
    builderError: HttpReadsBuilderError[ServiceError]
  ): ServiceError
}
