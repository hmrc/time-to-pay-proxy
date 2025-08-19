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

import cats.data.NonEmptyList
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.{ DefaultAwaitTimeout, FutureAwaits }
import play.api.{ ConfigLoader, Configuration }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.timetopayproxy.config.AppConfig
import uk.gov.hmrc.timetopayproxy.models.{ ConnectorError, IdType, IdValue, Identification }
import uk.gov.hmrc.timetopayproxy.models.TtppEnvelope.TtppEnvelope
import uk.gov.hmrc.timetopayproxy.models.chargeInfoApi._
import uk.gov.hmrc.timetopayproxy.support.WireMockUtils

import java.time.{ LocalDate, LocalDateTime }
import scala.concurrent.ExecutionContext

class TtpeConnectorSpec
    extends PlaySpec with DefaultAwaitTimeout with FutureAwaits with MockFactory with WireMockUtils {

  val config: Configuration = mock[Configuration]
  val servicesConfig: ServicesConfig = mock[ServicesConfig]

  val httpClient: HttpClientV2 = app.injector.instanceOf[HttpClientV2]

  class Setup {
    implicit val ec: ExecutionContext = ExecutionContext.global
    implicit val hc: HeaderCarrier = HeaderCarrier()

    (servicesConfig
      .baseUrl(_: String))
      .expects("auth")
      .returns("unused")
    (servicesConfig
      .baseUrl(_: String))
      .expects("ttp")
      .returns("unused")
    (servicesConfig
      .baseUrl(_: String))
      .expects("ttpe")
      .once()
      .returns("http://localhost:11111")
    (servicesConfig
      .baseUrl(_: String))
      .expects("stub")
      .returns("unused")
    (config
      .get(_: String)(_: ConfigLoader[String]))
      .expects("microservice.services.ttp.token", *)
      .returns("unused")
    (config
      .get(_: String)(_: ConfigLoader[Boolean]))
      .expects("microservice.services.ttp.useIf", *)
      .returns(false)
    (config
      .get(_: String)(_: ConfigLoader[Boolean]))
      .expects("auditing.enabled", *)
      .returns(false)
    (config
      .get(_: String)(_: ConfigLoader[String]))
      .expects("microservice.metrics.graphite.host", *)
      .returns("unused")
    (config
      .getOptional(_: String)(_: ConfigLoader[Option[Configuration]]))
      .expects("feature-switch", *)
      .returns(None)

    val mockConfiguration: AppConfig = new MockAppConfig(config, servicesConfig, ifImpl = false)

    val connector: TtpeConnector = new DefaultTtpeConnector(mockConfiguration, httpClient)

  }

  "DefaultTtpeConnector" when {

    def chargeInfoRequest: ChargeInfoRequest = ChargeInfoRequest(
      channelIdentifier = ChargeInfoChannelIdentifier("Channel Identifier"),
      identifications = NonEmptyList.of(
        Identification(idType = IdType("id type 1"), idValue = IdValue("id value 1")),
        Identification(idType = IdType("id type 2"), idValue = IdValue("id value 2"))
      ),
      regimeType = RegimeType.SA
    )

    def chargeInfoResponse: ChargeInfoResponse = ChargeInfoResponse(
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
            PostCodeInfo(addressPostcode = ChargeInfoPostCode("AB12 3CD"), postcodeDate = LocalDate.parse("2020-01-01"))
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

    ".checkChargeInfo" should {
      "make an http call to Charge Info API in Eligibility and retrieve the correct response" in new Setup {
        stubPostWithResponseBody(
          "/debts/time-to-pay/charge-info",
          200,
          Json.toJson(chargeInfoResponse).toString()
        )

        val result: TtppEnvelope[ChargeInfoResponse] = connector.checkChargeInfo(chargeInfoRequest = chargeInfoRequest)

        await(result.value) must matchPattern { case Right(_: ChargeInfoResponse) => }
      }

      "parse an error response from an upstream service" in new Setup {
        def errorResponse(code: String, reason: String): String =
          s"""
             |{
             |  "code":"$code",
             |  "reason":"$reason"
             |}
             |""".stripMargin

        stubPostWithResponseBody(
          "/debts/time-to-pay/charge-info",
          400,
          errorResponse("BAD_REQUEST", "Invalid request body")
        )

        val result: TtppEnvelope[ChargeInfoResponse] = connector.checkChargeInfo(chargeInfoRequest = chargeInfoRequest)

        await(result.value) mustBe Left(ConnectorError(400, "Invalid request body"))
      }
    }
  }
}
