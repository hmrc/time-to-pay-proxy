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

      /** This is used for 200 statuses. */
      def openApiCancelResponseSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          path,
          defaultJsonSubschemaName = "CancelResponse",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )

      /** This is used for 500 statuses. */
      def openApiCancelErrorResponseSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          path,
          defaultJsonSubschemaName = "CancelErrorResponse",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )

    }

    object TtpInform {
      // Downloaded from:
      //   https://jira.tools.tax.service.gov.uk/browse/DTD-2856
      // Official location:
      //   ???
      def openApiRequestSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          path,
          defaultJsonSubschemaName = "InformRequest",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )

      /** This is used for 200 statuses. */
      def openApiInformResponseSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          path,
          defaultJsonSubschemaName = "InformResponse",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )

      /** This is used for 500 statuses. */
      def openApiInformErrorResponseSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          path,
          defaultJsonSubschemaName = "InformErrorResponse",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )
    }

    object TtpInformR2 {
      // Downloaded from:
      //   https://jira.tools.tax.service.gov.uk/browse/DTD-2856
      // Official location:
      //   ???
      private val pathR2: String = "resources/public/api/conf/1.0/time-to-pay-v1.0.20-proposedAll-R2.yaml"
      def openApiRequestSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          pathR2,
          defaultJsonSubschemaName = "InformRequest",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )

      /** This is used for 200 statuses. */
      def openApiInformResponseSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          pathR2,
          defaultJsonSubschemaName = "InformResponse",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )

      /** This is used for 500 statuses. */
      def openApiInformErrorResponseSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          pathR2,
          defaultJsonSubschemaName = "InformErrorResponse",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )
    }

    object TtpFullAmend {
      // Downloaded from:
      //   https://confluence.tools.tax.service.gov.uk/display/DTDT/TTP+API+%28Current+Version%29+Proxy?preview=/828113579/1168278271/time-to-pay-v1.0.19.yaml
      // Official location:
      //   https://confluence.tools.tax.service.gov.uk/display/DTDT/TTP+API+%28Current+Version%29+Proxy
      def openApiRequestSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          path,
          defaultJsonSubschemaName = "FullAmendRequest",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )

      /** This is used for 200 statuses. */
      def openApiFullAmendResponseSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          path,
          defaultJsonSubschemaName = "FullAmendResponse",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )

      /** This is used for 500 statuses. */
      def openApiFullAmendErrorResponseSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          path,
          defaultJsonSubschemaName = "FullAmendErrorResponse",
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

      def openApiCancelResponseSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          path,
          defaultJsonSubschemaName = "CancelResponse",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )

      def openApiCancelErrorResponseSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          path,
          defaultJsonSubschemaName = "CancelErrorResponse",
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

    object TtpInform {
      // Downloaded from:
      //   https://github.com/hmrc/time-to-pay/blob/af176ffca7a0bb85bb6c180d9eff0d2d6755a37a/resources/public/api/conf/1.0/InformAPI-v0.0.1.yaml
      // Official location:
      //   https://github.com/hmrc/time-to-pay/blob/main/resources/public/api/conf/1.0
      private val path = "test/resources/schemas/apis/time-to-pay/InformAPI-v0.0.1.yaml"

      def openApiRequestSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          path,
          defaultJsonSubschemaName = "InformRequest",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )

      def openApiInformErrorResponseSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          path,
          defaultJsonSubschemaName = "InformErrorResponse",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )

      def openApiInformResponseSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          path,
          defaultJsonSubschemaName = "InformResponse",
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

    object TtpInformR2 {
      // Downloaded from:
      //   https://github.com/hmrc/time-to-pay/blob/af176ffca7a0bb85bb6c180d9eff0d2d6755a37a/resources/public/api/conf/1.0/InformAPI-v0.0.1.yaml
      // Official location:
      //   https://github.com/hmrc/time-to-pay/blob/main/resources/public/api/conf/1.0
      private val path = "test/resources/schemas/apis/time-to-pay/InformAPI-v0.0.3-proposed.yaml"

      def openApiRequestSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          path,
          defaultJsonSubschemaName = "InformRequest",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )

      def openApiInformErrorResponseSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          path,
          defaultJsonSubschemaName = "InformErrorResponse",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )

      def openApiInformResponseSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          path,
          defaultJsonSubschemaName = "InformResponse",
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


    object TtpFullAmend {
      // Downloaded from:
      //   https://github.com/hmrc/time-to-pay/blob/fe8fee3d7dead5d6241d7d085cff4179ea5834ef/resources/public/api/conf/1.0/FullAmendAPI-v0.0.1.yaml
      // Official location:
      //   https://github.com/hmrc/time-to-pay/tree/main/resources/public/api/conf/1.0
      private val path = "test/resources/schemas/apis/time-to-pay/FullAmendAPI-v0.0.1.yaml"

      def openApiRequestSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          path,
          defaultJsonSubschemaName = "FullAmendRequest",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )

      def openApiFullAmendErrorResponseSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          path,
          defaultJsonSubschemaName = "FullAmendErrorResponse",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )

      def openApiFullAmendResponseSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          path,
          defaultJsonSubschemaName = "FullAmendResponse",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )

      def openApiFullAmendResponseGeneralFailureSchema: OpenApi3DerivedSchema =
        new OpenApi3DerivedSchema(
          path,
          defaultJsonSubschemaName = "ErrorResponse",
          metaSchemaValidation = Some(Valid(())),
          restrictAdditionalProperties = true
        )
    }
  }

}
