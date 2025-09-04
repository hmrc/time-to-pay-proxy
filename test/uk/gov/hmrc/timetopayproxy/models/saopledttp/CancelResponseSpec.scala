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

package uk.gov.hmrc.timetopayproxy.models.saopledttp

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json._

import java.time.Instant

class CancelResponseSpec extends AnyWordSpec with Matchers {

  private val processingDateTime = ProcessingDateTime(Instant.parse("2025-10-15T10:31:00Z"))
  private val apiStatusSuccess = ApiStatus(
    name = ApiName("CESA"),
    statusCode = ApiStatusCode("200"),
    processingDateTime = ProcessingDateTime(Instant.parse("2025-05-01T14:30:00Z")),
    errorResponse = None
  )
  private val apiStatusError = ApiStatus(
    name = ApiName("CESA"),
    statusCode = ApiStatusCode("400"),
    processingDateTime = ProcessingDateTime(Instant.parse("2025-10-15T10:30:00Z")),
    errorResponse = Some(ApiErrorResponse("Invalid cancellationDate"))
  )

  "CancelResponse" when {
    "serializing to JSON" should {
      "produce correct JSON structure for successful response" in {
        val response = CancelResponse(
          apisCalled = List(apiStatusSuccess),
          processingDateTime = processingDateTime
        )

        val json = Json.toJson(response)

        (json \ "apisCalled").as[JsArray].value should have length 1
        (json \ "apisCalled" \ 0 \ "name").as[String] shouldBe "CESA"
        (json \ "apisCalled" \ 0 \ "statusCode").as[String] shouldBe "200"
        (json \ "apisCalled" \ 0 \ "processingDateTime").as[String] shouldBe "2025-05-01T14:30:00Z"
        (json \ "apisCalled" \ 0 \ "errorResponse").asOpt[String] shouldBe None

        (json \ "processingDateTime").as[String] shouldBe "2025-10-15T10:31:00Z"
      }

      "produce correct JSON structure for error response" in {
        val response = CancelResponse(
          apisCalled = List(apiStatusError),
          processingDateTime = processingDateTime
        )

        val json = Json.toJson(response)

        (json \ "apisCalled").as[JsArray].value should have length 1
        (json \ "apisCalled" \ 0 \ "name").as[String] shouldBe "CESA"
        (json \ "apisCalled" \ 0 \ "statusCode").as[String] shouldBe "400"
        (json \ "apisCalled" \ 0 \ "processingDateTime").as[String] shouldBe "2025-10-15T10:30:00Z"
        (json \ "apisCalled" \ 0 \ "errorResponse").as[String] shouldBe "Invalid cancellationDate"

        (json \ "processingDateTime").as[String] shouldBe "2025-10-15T10:31:00Z"
      }
    }

    "deserializing from JSON" should {
      "parse valid successful response JSON correctly" in {
        val json = Json.obj(
          "apisCalled" -> Json.arr(
            Json.obj(
              "name"               -> "CESA",
              "statusCode"         -> "200",
              "processingDateTime" -> "2025-05-01T14:30:00Z"
            )
          ),
          "processingDateTime" -> "2025-10-15T10:31:00Z"
        )

        val result = json.validate[CancelResponse]
        result shouldBe a[JsSuccess[_]]

        val response = result.get
        response.apisCalled should have length 1
        response.apisCalled.head.name.value shouldBe "CESA"
        response.apisCalled.head.statusCode.value shouldBe "200"
        response.apisCalled.head.errorResponse shouldBe None
      }

      "parse valid error response JSON correctly" in {
        val json = Json.obj(
          "apisCalled" -> Json.arr(
            Json.obj(
              "name"               -> "CESA",
              "statusCode"         -> "400",
              "processingDateTime" -> "2025-10-15T10:30:00Z",
              "errorResponse"      -> "Invalid cancellationDate"
            )
          ),
          "processingDateTime" -> "2025-10-15T10:31:00Z"
        )

        val result = json.validate[CancelResponse]
        result shouldBe a[JsSuccess[_]]

        val response = result.get
        response.apisCalled should have length 1
        response.apisCalled.head.name.value shouldBe "CESA"
        response.apisCalled.head.statusCode.value shouldBe "400"
        response.apisCalled.head.errorResponse.map(_.value) shouldBe Some("Invalid cancellationDate")
      }

      "fail validation for invalid JSON structure" in {
        val json = Json.obj(
          "apisCalled"         -> "not an array",
          "processingDateTime" -> "2025-10-15T10:31:00Z"
        )

        val result = json.validate[CancelResponse]
        result shouldBe a[JsError]
      }
    }
  }

  "ApiStatus" when {
    "serializing to JSON" should {
      "handle optional errorResponse field" in {
        val status = ApiStatus(
          name = ApiName("CESA"),
          statusCode = ApiStatusCode("200"),
          processingDateTime = ProcessingDateTime(Instant.parse("2025-05-01T14:30:00Z")),
          errorResponse = None
        )

        val json = Json.toJson(status)
        (json \ "errorResponse").asOpt[String] shouldBe None
      }

      "include errorResponse when present" in {
        val status = ApiStatus(
          name = ApiName("CESA"),
          statusCode = ApiStatusCode("400"),
          processingDateTime = ProcessingDateTime(Instant.parse("2025-05-01T14:30:00Z")),
          errorResponse = Some(ApiErrorResponse("Some error"))
        )

        val json = Json.toJson(status)
        (json \ "errorResponse").as[String] shouldBe "Some error"
      }
    }
  }
}
