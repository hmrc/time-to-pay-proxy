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

package uk.gov.hmrc.timetopayproxy.testutils.schematestutils

import cats.data.Validated.Valid
import uk.gov.hmrc.timetopayproxy.testutils.schematestutils.impl.DebtTransSchemaValidator.OpenApi3DerivedSchema

object Validators {

  object TimeToPayProxy {
    // Downloaded from:
    //   https://jira.tools.tax.service.gov.uk/browse/DTD-3634
    //   https://confluence.tools.tax.service.gov.uk/display/DTDT/TTP+API+%28Current+Version%29+Proxy?preview=/828113579/1116832157/time-to-pay-v1.0.12-proposedB.yaml
    // Official location:
    //   https://confluence.tools.tax.service.gov.uk/display/DTDT/TTP+API+%28Current+Version%29+Proxy
    private val path: String = "resources/public/api/conf/1.0/application.yaml"

    /** This goes to `time-to-pay-eligibility`. */
    object ChargeInfo {
      def openApiRequestSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          openApiYamlFilename = path,
          defaultJsonSubschemaName = "TTPChargeInfoRequest",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )

      def openApiResponseSuccessfulSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          openApiYamlFilename = path,
          defaultJsonSubschemaName = "TTPChargeInfoResponse",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )
    }

    object AffordableQuotes {
      def openApiRequestSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          openApiYamlFilename = path,
          defaultJsonSubschemaName = "AffordableQuotesRequest",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )

      def openApiResponseSuccessfulSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          openApiYamlFilename = path,
          defaultJsonSubschemaName = "AffordableQuotesResponse",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )
    }

    object TtpCancel {
      // Downloaded from:
      //   https://jira.tools.tax.service.gov.uk/browse/DTD-2858
      // Official location:
      //   ???
      def openApiRequestSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          path,
          defaultJsonSubschemaName = "CancelRequest",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )

      /** This is used for both 200 and 500 statuses. */
      // TODO DTD-3785: Split into the separate schemas for 200 and 500 responses. 500 will have an extra field.
      def openApiInformativeResponseSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          path,
          defaultJsonSubschemaName = "CancelResponse",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )

      def openApiResponseGeneralFailureSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          path,
          defaultJsonSubschemaName = "CancelErrorResponse",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )
    }

    /** For now, this applies to all responses from the proxy */
    def openApiResponseErrorSchema: OpenApi3DerivedSchema =
      new OpenApi3DerivedSchema(
        openApiYamlFilename = path,
        defaultJsonSubschemaName = "ErrorResponse",
        metaSchemaValidation = Some(Valid(())),
        restrictAdditionalProperties = true
      )
  }

  object TimeToPayEligibility {
    // Downloaded from:
    //   https://github.com/hmrc/time-to-pay-eligibility/blob/dfc3c970abfe7ee8bafbd32e2dc77d35b829a37b/test/resources/schemas/apis/time-to-pay-eligibility/ttp-eligibility0.3.6.yaml
    // Official location:
    //   https://confluence.tools.tax.service.gov.uk/display/DTDT/TTP+Eligibility+API
    private val path = "test/resources/schemas/apis/time-to-pay-eligibility/ttp-eligibility0.3.6.yaml"

    object ChargeInfo {
      def openApiRequestSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          openApiYamlFilename = path,
          defaultJsonSubschemaName = "TTPEligibilityRequestChargeInfo",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )

      def openApiResponseSuccessfulSchema =
        new OpenApi3DerivedSchema(
          openApiYamlFilename = path,
          defaultJsonSubschemaName = "TTPEligibilitySuccessResponseChargeInfo",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )

      def openApiErrorSchema =
        new OpenApi3DerivedSchema(
          openApiYamlFilename = path,
          defaultJsonSubschemaName = "ErrorResponse",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )
    }
  }

  object TimeToPay {
    object AffordableQuotes {
      // Downloaded from:
      //   https://github.com/hmrc/time-to-pay/blob/82c464a0fa7ba93ac869ee361f497fd92a206ed6/resources/public/api/conf/1.0/affordable-quotes-api-v1.3.0.yaml
      // Official location:
      //   https://github.com/hmrc/time-to-pay/tree/main/resources/public/api/conf/1.0
      private val path = "test/resources/schemas/apis/time-to-pay/affordable-quotes-api-v1.3.0.yaml"

      def openApiRequestSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          path,
          defaultJsonSubschemaName = "TTPAffordableQuotesRequest",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )

      def openApiResponseSuccessfulSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          path,
          defaultJsonSubschemaName = "TTPAffordableQuotesResponse",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )
    }

    object TtpCancel {
      // Downloaded from:
      //   https://github.com/hmrc/time-to-pay/blob/7d0903506524411871930c3a3dc81899b58c7985/resources/public/api/conf/1.0/CancelAPI-v0.0.1.yaml
      // Official location:
      //   https://github.com/hmrc/time-to-pay/tree/main/resources/public/api/conf/1.0
      private val path = "test/resources/schemas/apis/time-to-pay/CancelAPI-v0.0.1.yaml"

      def openApiRequestSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          path,
          defaultJsonSubschemaName = "CancelRequest",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )

      def openApiInformativeResponseSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          path,
          defaultJsonSubschemaName = "CancelResponse",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )

      def openApiResponseGeneralFailureSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          path,
          defaultJsonSubschemaName = "ErrorResponse",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )
    }
  }

}
