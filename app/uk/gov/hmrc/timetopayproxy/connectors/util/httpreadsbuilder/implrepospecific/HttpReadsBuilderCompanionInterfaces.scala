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

package uk.gov.hmrc.timetopayproxy.connectors.util.httpreadsbuilder.implrepospecific

import uk.gov.hmrc.timetopayproxy.connectors.util.httpreadsbuilder.HttpReadsBuilder

/** The repo-specific interface of [[HttpReadsBuilder]]. Separated out so it can be maintained more easily across repos. */
private[httpreadsbuilder] trait HttpReadsBuilderCompanionInterfaces { this: HttpReadsBuilder.type =>

  /** See [[ConverterDefaultingTo503]] for what errors this `HttpReads` will generate. */
  def withDefault503ConnectorError[ServiceError >: ConverterDefaultingTo503.ServiceErrorLowerBound, Result](
    sourceClass: Class[_]
  ): HttpReadsBuilder[ServiceError, Result] =
    HttpReadsBuilder.empty(
      sourceClass = sourceClass,
      converter = ConverterDefaultingTo503
    )
}
