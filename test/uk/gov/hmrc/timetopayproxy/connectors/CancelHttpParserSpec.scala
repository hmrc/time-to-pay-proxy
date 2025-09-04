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

package uk.gov.hmrc.timetopayproxy.connectors

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.timetopayproxy.models.ConnectorError
import uk.gov.hmrc.timetopayproxy.models.saopledttp.CancelResponseError

import java.time.Instant

class CancelHttpParserSpec extends AnyWordSpec with Matchers {

  "CancelHttpParser" when {
    "handling 200 OK response" should {
      "parse valid CancelResponse correctly" in {
        val responseJson = Json.parse("""
          {
            "apisCalled": [
              {
                "name": "CESA",
                "statusCode": "200",
                "processingDateTime": "2025-05-01T14:30:00Z"
              }
            ],
            "processingDateTime": "2025-10-15T10:31:00Z"
          }
        """)

        val httpResponse = HttpResponse(
          status = Status.OK,
          json = responseJson,
          headers = Map.empty
        )

        val result = CancelHttpParser.httpReads.read("GET", "url", httpResponse)

        result shouldBe a[Right[_, _]]
        val cancelResponse = result.getOrElse(fail("Expected Right but got Left"))
        cancelResponse.apisCalled.length shouldBe 1
        cancelResponse.apisCalled.head.name.value shouldBe "CESA"
        cancelResponse.apisCalled.head.statusCode.value shouldBe "200"
        cancelResponse.processingDateTime.value shouldBe Instant.parse("2025-10-15T10:31:00Z")
      }

      "return error for invalid JSON structure" in {
        val invalidJson = Json.parse("""{"invalid": "structure"}""")

        val httpResponse = HttpResponse(
          status = Status.OK,
          json = invalidJson,
          headers = Map.empty
        )

        val result = CancelHttpParser.httpReads.read("GET", "url", httpResponse)

        result shouldBe a[Left[_, _]]
        val error = result.swap.getOrElse(fail("Expected Left but got Right")).asInstanceOf[ConnectorError]
        error.statusCode shouldBe 503
        error.message shouldBe "Couldn't parse body from upstream"
      }
    }

    "handling 500 Internal Server Error response" should {
      "parse valid CancelResponse with error details" in {
        val responseJson = Json.parse("""
          {
            "apisCalled": [
              {
                "name": "CESA",
                "statusCode": "400",
                "processingDateTime": "2025-10-15T10:30:00Z",
                "errorResponse": "Invalid cancellationDate"
              }
            ],
            "processingDateTime": "2025-10-15T10:31:00Z"
          }
        """)

        val httpResponse = HttpResponse(
          status = Status.INTERNAL_SERVER_ERROR,
          json = responseJson,
          headers = Map.empty
        )

        val result = CancelHttpParser.httpReads.read("GET", "url", httpResponse)

        result shouldBe a[Left[_, _]]
        val error = result.swap.getOrElse(fail("Expected Left but got Right")).asInstanceOf[CancelResponseError]
        error.statusCode shouldBe 500
        error.cancelResponse.apisCalled.head.statusCode.value shouldBe "400"
        error.cancelResponse.apisCalled.head.errorResponse.map(_.value) shouldBe Some("Invalid cancellationDate")
      }

      "return error for invalid JSON structure" in {
        val invalidJson = Json.parse("""{"invalid": "structure"}""")

        val httpResponse = HttpResponse(
          status = Status.INTERNAL_SERVER_ERROR,
          json = invalidJson,
          headers = Map.empty
        )

        val result = CancelHttpParser.httpReads.read("GET", "url", httpResponse)

        result shouldBe a[Left[_, _]]
        val error = result.swap.getOrElse(fail("Expected Left but got Right")).asInstanceOf[ConnectorError]
        error.statusCode shouldBe 503
        error.message shouldBe "Couldn't parse body from upstream"
      }
    }

    "handling 400 Bad Request response" should {
      "parse valid CancelErrorResponse correctly" in {
        val responseJson = Json.parse("""
          {
            "code": 400,
            "details": "Invalid request payload: missing identifications or cancellationDate"
          }
        """)

        val httpResponse = HttpResponse(
          status = Status.BAD_REQUEST,
          json = responseJson,
          headers = Map.empty
        )

        val result = CancelHttpParser.httpReads.read("GET", "url", httpResponse)

        result shouldBe a[Left[_, _]]
        val error = result.swap.getOrElse(fail("Expected Left but got Right")).asInstanceOf[ConnectorError]
        error.statusCode shouldBe 400
        error.message shouldBe "Invalid request payload: missing identifications or cancellationDate"
      }

      "return error for invalid JSON structure" in {
        val invalidJson = Json.parse("""{"invalid": "structure"}""")

        val httpResponse = HttpResponse(
          status = Status.BAD_REQUEST,
          json = invalidJson,
          headers = Map.empty
        )

        val result = CancelHttpParser.httpReads.read("GET", "url", httpResponse)

        result shouldBe a[Left[_, _]]
        val error = result.swap.getOrElse(fail("Expected Left but got Right")).asInstanceOf[ConnectorError]
        error.statusCode shouldBe 503
        error.message shouldBe "Couldn't parse body from upstream"
      }

      "handle non-JSON response" in {
        val httpResponse = HttpResponse(
          status = Status.BAD_REQUEST,
          body = "Not JSON",
          headers = Map.empty
        )

        val result = CancelHttpParser.httpReads.read("GET", "url", httpResponse)

        result shouldBe a[Left[_, _]]
        val error = result.swap.getOrElse(fail("Expected Left but got Right")).asInstanceOf[ConnectorError]
        error.statusCode shouldBe 400
        error.message shouldBe "Couldn't parse body from upstream"
      }
    }

    "handling unexpected status codes" should {
      "return error for unexpected status" in {
        val httpResponse = HttpResponse(
          status = Status.NOT_FOUND,
          json = Json.obj(),
          headers = Map.empty
        )

        val result = CancelHttpParser.httpReads.read("GET", "url", httpResponse)

        result shouldBe a[Left[_, _]]
        val error = result.swap.getOrElse(fail("Expected Left but got Right")).asInstanceOf[ConnectorError]
        error.statusCode shouldBe 404
        error.message shouldBe "Unexpected response from upstream"
      }

      "return error for service unavailable" in {
        val httpResponse = HttpResponse(
          status = Status.SERVICE_UNAVAILABLE,
          json = Json.obj(),
          headers = Map.empty
        )

        val result = CancelHttpParser.httpReads.read("GET", "url", httpResponse)

        result shouldBe a[Left[_, _]]
        val error = result.swap.getOrElse(fail("Expected Left but got Right")).asInstanceOf[ConnectorError]
        error.statusCode shouldBe 503
        error.message shouldBe "Unexpected response from upstream"
      }
    }
  }
}
