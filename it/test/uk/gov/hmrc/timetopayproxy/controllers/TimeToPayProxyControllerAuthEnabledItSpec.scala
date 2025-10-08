/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.timetopayproxy.controllers

import cats.data.NonEmptyList
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{ equalTo, postRequestedFor, urlPathEqualTo }
import play.api.libs.json.Json
import play.api.libs.ws.{ WSRequest, WSResponse }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.timetopayproxy.models._
import uk.gov.hmrc.timetopayproxy.models.saonly.chargeInfoApi._
import uk.gov.hmrc.timetopayproxy.models.saonly.common._
import uk.gov.hmrc.timetopayproxy.support.IntegrationBaseSpec

import java.time.{ LocalDate, LocalDateTime }
import scala.concurrent.ExecutionContext

class TimeToPayProxyControllerAuthEnabledItSpec extends IntegrationBaseSpec {
  def internalAuthEnabled: Boolean = true

  implicit def ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  implicit val hc: HeaderCarrier = HeaderCarrier()

  "TimeToPayProxyController" - {
    ".checkChargeInfo" - {
      val chargeInfoRequest: ChargeInfoRequest = ChargeInfoRequest(
        channelIdentifier = ChargeInfoChannelIdentifier("Channel Identifier"),
        identifications = NonEmptyList.of(
          Identification(idType = IdType("id type 1"), idValue = IdValue("id value 1")),
          Identification(idType = IdType("id type 2"), idValue = IdValue("id value 2"))
        ),
        regimeType = SaOnlyRegimeType.SA
      )

      val chargeInfoResponse: ChargeInfoResponse = ChargeInfoResponse(
        processingDateTime = LocalDateTime.parse("2025-07-02T15:00:41.689"),
        identification = List(
          Identification(idType = IdType("ID_TYPE"), idValue = IdValue("ID_VALUE"))
        ),
        individualDetails = IndividualDetails(
          title = Some(Title("Mr")),
          firstName = Some(FirstName("John")),
          lastName = Some(LastName("Doe")),
          dateOfBirth = Some(DateOfBirth(LocalDate.parse("1980-01-01"))),
          districtNumber = Some(DistrictNumber("1234")),
          customerType = CustomerType.ItsaMigtrated,
          transitionToCDCS = TransitionToCdcs(value = true)
        ),
        addresses = List(
          Address(
            addressType = AddressType("Address Type"),
            addressLine1 = AddressLine1("Address Line 1"),
            addressLine2 = Some(AddressLine2("Address Line 2")),
            addressLine3 = Some(AddressLine3("Address Line 3")),
            addressLine4 = Some(AddressLine4("Address Line 4")),
            rls = Some(Rls(true)),
            contactDetails = Some(
              ContactDetails(
                telephoneNumber = Some(TelephoneNumber("telephone-number")),
                fax = Some(Fax("fax-number")),
                mobile = Some(Mobile("mobile-number")),
                emailAddress = Some(Email("email address")),
                emailSource = Some(EmailSource("email source"))
              )
            ),
            postCode = Some(ChargeInfoPostCode("AB12 3CD")),
            postcodeHistory = List(
              PostCodeInfo(
                addressPostcode = ChargeInfoPostCode("AB12 3CD"),
                postcodeDate = LocalDate.parse("2020-01-01")
              )
            )
          )
        ),
        chargeTypeAssessment = List(
          ChargeTypeAssessment(
            debtTotalAmount = BigInt(1000),
            chargeReference = ChargeReference("CHARGE REFERENCE"),
            parentChargeReference = Some(ChargeInfoParentChargeReference("PARENT CHARGE REF")),
            mainTrans = MainTrans("2000"),
            charges = List(
              Charge(
                taxPeriodFrom = TaxPeriodFrom(LocalDate.parse("2020-01-02")),
                taxPeriodTo = TaxPeriodTo(LocalDate.parse("2020-12-31")),
                chargeType = ChargeType("charge type"),
                mainType = MainType("main type"),
                subTrans = SubTrans("1000"),
                outstandingAmount = OutstandingAmount(BigInt(500)),
                dueDate = DueDate(LocalDate.parse("2021-01-31")),
                isInterestBearingCharge = Some(ChargeInfoIsInterestBearingCharge(true)),
                interestStartDate = Some(InterestStartDate(LocalDate.parse("2020-01-03"))),
                accruedInterest = AccruedInterest(BigInt(50)),
                chargeSource = ChargeInfoChargeSource("Source"),
                parentMainTrans = Some(ChargeInfoParentMainTrans("Parent Main Transaction")),
                originalCreationDate = Some(OriginalCreationDate(LocalDate.parse("2025-07-02"))),
                tieBreaker = Some(TieBreaker("Tie Breaker")),
                originalTieBreaker = Some(OriginalTieBreaker("Original Tie Breaker")),
                saTaxYearEnd = Some(SaTaxYearEnd(LocalDate.parse("2020-04-05"))),
                creationDate = Some(CreationDate(LocalDate.parse("2025-07-02"))),
                originalChargeType = Some(OriginalChargeType("Original Charge Type"))
              )
            )
          )
        )
      )

      "should send a request with an Authorization header" - {
        "and return a 200" in {
          stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")
          stubPostWithResponseBody(
            url = "/debts/time-to-pay/charge-info",
            status = 200,
            responseBody = Json.toJson(chargeInfoResponse).toString(),
            requestHeaderContaining = Some(Seq("Authorization" -> equalTo("configured-auth-token")))
          )

          val requestForChargeInfo: WSRequest = buildRequest("/charge-info")
          val response: WSResponse = await(
            requestForChargeInfo.post(Json.toJson(chargeInfoRequest))
          )

          WireMock.verify(1, postRequestedFor(urlPathEqualTo("/debts/time-to-pay/charge-info")))

          response.json.as[ChargeInfoResponse] shouldBe chargeInfoResponse
          response.status shouldBe 200
        }
      }
    }
  }
}
