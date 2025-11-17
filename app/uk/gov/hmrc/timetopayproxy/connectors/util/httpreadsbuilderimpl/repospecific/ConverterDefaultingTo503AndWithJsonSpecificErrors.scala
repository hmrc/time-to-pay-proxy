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

import uk.gov.hmrc.timetopayproxy.connectors.util.httpreadsbuilderimpl.commontoallrepos.{ HttpReadsBuilderError, HttpReadsBuilderErrorConverter }
import uk.gov.hmrc.timetopayproxy.connectors.util.httpreadsbuilderimpl.commontoallrepos.HttpReadsBuilderError.Impl
import uk.gov.hmrc.timetopayproxy.models.error.ConnectorError

/** <li>Returns a 503 error when an unexpected status code is received.</li>
  * <li>Returns special error classes when expected JSON responses can't be understood.</li>
  * <li>Returns a special error class when an unexpected HTTP body is returned and we expected no body.</li>
  */
private[util] object ConverterDefaultingTo503AndWithJsonSpecificErrors
    extends HttpReadsBuilderErrorConverter[ConnectorError] {

  type ServErrorLowerBound = ConnectorError

  /** `ServError` is what we expect to have to convert, which has to include every possible `ConnectorError`. */
  def toConnectorError[ServError >: ConnectorError](builderError: HttpReadsBuilderError[ServError]): ServError =
    builderError match {
      case variant: Impl.PassthroughServiceError[ServError] =>
        variant.error: ServError
      case variant: Impl.GeneralErrorForUnsuccessfulStatusCode =>
        ConnectorError(
          // The ConnectorError statusCode field will be forwarded to our clients. It's not the status code we received.
          // Hardcoded to 503 because unknown upstream status codes cannot be blindly forwarded, because:
          //   1. They will likely break our own schema by returning undocumented status codes with mismatched JSON.
          //   2. Some status codes don't make sense to be forwarded except in very special situations, e.g. 403, 404.
          statusCode = 503,
          message = s"Status code ${variant.responseContext.response.status}: ${variant.simpleMessage}"
        )
      case variant: Impl.NotJsonErrorForSuccess =>
        import variant.responseContext
        ConnectorError(
          statusCode = 503,
          message =
            s"Received status code ${responseContext.response.status} with non-JSON body for request: ${responseContext.method} ${responseContext.url}"
        )
      case variant: Impl.JsonNotValidErrorForSuccess =>
        import variant.responseContext
        ConnectorError(
          statusCode = 503,
          message =
            s"""Received status code ${responseContext.response.status} with incorrect JSON body for request: ${responseContext.method} ${responseContext.url}"""
        )
      case variant: Impl.BodyNotEmptyErrorForSuccess =>
        import variant.responseContext
        ConnectorError(
          statusCode = 503,
          message =
            s"Received status code ${responseContext.response.status} with non-empty body when none was expected for request: ${responseContext.method} ${responseContext.url}"
        )
    }

}
