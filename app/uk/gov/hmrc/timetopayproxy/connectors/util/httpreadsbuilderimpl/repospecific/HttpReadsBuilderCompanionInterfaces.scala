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

package uk.gov.hmrc.timetopayproxy.connectors.util.httpreadsbuilderimpl.repospecific

import uk.gov.hmrc.timetopayproxy.connectors.util.HttpReadsBuilder

/** The repo-specific interface of [[HttpReadsBuilder]]. Separated out so it can be maintained more easily across repos. */
private[util] trait HttpReadsBuilderCompanionInterfaces { this: HttpReadsBuilder.type =>

  /** See [[ConverterDefaultingTo503AndWithJsonSpecificErrors]] for what errors this `HttpReads` will generate. */
  def withDefault503ConnectorErrorAndJsonErrors[
    ServError >: ConverterDefaultingTo503AndWithJsonSpecificErrors.ServErrorLowerBound,
    Result
  ](
    sourceClass: Class[_]
  ): HttpReadsBuilder[ServError, Result] =
    HttpReadsBuilder.empty(
      sourceClass = sourceClass,
      converter = ConverterDefaultingTo503AndWithJsonSpecificErrors
    )
}
