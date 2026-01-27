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
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.http.RequestMethod.{ GET, POST, PUT }
import play.api.libs.json.{ Json, Reads }
import play.api.libs.ws.{ WSRequest, WSResponse }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.timetopayproxy.config.FeatureSwitch
import uk.gov.hmrc.timetopayproxy.models._
import uk.gov.hmrc.timetopayproxy.models.affordablequotes.{ AffordableQuoteResponse, AffordableQuotesRequest }
import uk.gov.hmrc.timetopayproxy.models.currency.GbpPounds
import uk.gov.hmrc.timetopayproxy.models.featureSwitches.SARelease2Enabled
import uk.gov.hmrc.timetopayproxy.models.saonly.chargeInfoApi._
import uk.gov.hmrc.timetopayproxy.models.saonly.common._
import uk.gov.hmrc.timetopayproxy.models.saonly.common.apistatus.{ ApiErrorResponse, ApiName, ApiStatus, ApiStatusCode }
import uk.gov.hmrc.timetopayproxy.models.saonly.ttpcancel.{ CancellationDate, TtpCancelPaymentPlan, TtpCancelRequest, TtpCancelSuccessfulResponse }
import uk.gov.hmrc.timetopayproxy.models.saonly.ttpfullamend.{ TtpFullAmendRequestR1, TtpFullAmendSuccessfulResponse }
import uk.gov.hmrc.timetopayproxy.models.saonly.ttpinform.{ TtpInformRequest, TtpInformSuccessfulResponse }
import uk.gov.hmrc.timetopayproxy.support.IntegrationBaseSpec

import java.time.{ Instant, LocalDate, LocalDateTime }
import scala.concurrent.ExecutionContext

class TimeToPayProxyControllerEnrolmentAuthEnabledItSpec extends IntegrationBaseSpec {
  def internalAuthEnabled: Boolean = false
  def enrolmentAuthEnabled: Boolean = true

  implicit def ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  implicit val hc: HeaderCarrier = HeaderCarrier()

  "TimeToPayProxyController" - {
    ".generateQuote" - {
      val requestPayload: GenerateQuoteRequest =
        GenerateQuoteRequest(
          CustomerReference("customerReference"),
          ChannelIdentifier.Advisor,
          PlanToGenerateQuote(
            QuoteType.Duration,
            quoteDate = LocalDate.parse("2010-02-02"),
            instalmentStartDate = LocalDate.parse("2010-02-02"),
            instalmentAmount = Some(1),
            Some(FrequencyLowercase.Annually),
            Some(Duration(12)),
            initialPaymentAmount = Some(1),
            initialPaymentDate = Some(LocalDate.parse("2010-02-02")),
            PaymentPlanType.TimeToPay
          ),
          customerPostCodes = List(),
          debtItemCharges = List(),
          regimeType = None
        )

      val responsePayload: GenerateQuoteResponse =
        GenerateQuoteResponse(
          QuoteReference("quoteReference"),
          CustomerReference("customerReference"),
          QuoteType.Duration,
          quoteDate = LocalDate.parse("2010-02-02"),
          numberOfInstalments = 1,
          totalDebtIncInt = 100,
          interestAccrued = 0.6,
          planInterest = 0.9,
          totalInterest = 0.9,
          instalments = List(
            Instalment(
              DebtItemChargeId("dutyId"),
              dueDate = LocalDate.parse("2022-01-01"),
              amountDue = 100,
              expectedPayment = 100,
              interestRate = 0.1,
              instalmentNumber = 1,
              instalmentInterestAccrued = 0.5,
              instalmentBalance = 10
            )
          ),
          collections = Collections(
            Some(InitialCollection(LocalDate.parse("2010-02-02"), 1)),
            List(RegularCollection(LocalDate.parse("2022-01-01"), 100))
          )
        )

      "should send the enrolment scope to the authorise endpoint" - {
        "and return a 200" in {
          stubRequest(
            httpMethod = POST,
            url = "/auth/authorise",
            status = 200,
            responseBody = "null",
            requestBodyContaining = Some(
              Json
                .parse(
                  """{"authorise":[{"identifiers":[],"state":"Activated","enrolment":"read:time-to-pay-proxy"}],"retrieve":[]}"""
                )
                .toString()
            )
          )

          stubRequest(
            httpMethod = POST,
            url = "/debts/time-to-pay/quote",
            status = 201,
            responseBody = Json.toJson(responsePayload).toString()
          )

          val request: WSRequest = buildRequest("/quote")
          val response: WSResponse = await(
            request.post(Json.toJson(requestPayload))
          )

          WireMock.verify(1, postRequestedFor(urlPathEqualTo("/auth/authorise")))
          WireMock.verify(1, postRequestedFor(urlPathEqualTo("/debts/time-to-pay/quote")))

          response.json.as[GenerateQuoteResponse] shouldBe responsePayload
          response.status shouldBe 200
        }
      }
    }

    ".viewPlan" - {
      val responsePayload: ViewPlanResponse =
        ViewPlanResponse(
          CustomerReference("customerRef1234"),
          ChannelIdentifier.Advisor,
          ViewPlanResponsePlan(
            PlanId("planId123"),
            CaseId("caseId123"),
            QuoteId("quoteId"),
            quoteDate = LocalDate.parse("2010-02-02"),
            QuoteType.InstalmentAmount,
            PaymentPlanType.TimeToPay,
            thirdPartyBank = true,
            numberOfInstalments = 0,
            initialPaymentMethod = None,
            initialPaymentReference = None,
            totalDebtIncInt = 0,
            totalInterest = 0.0,
            interestAccrued = 0,
            planInterest = 0.0
          ),
          Seq(
            DebtItemCharge(
              DebtItemChargeId("debtItemChargeId1"),
              mainTrans = "1546",
              subTrans = "1090",
              originalDebtAmount = 100,
              interestStartDate = Some(LocalDate.parse("2021-05-13")),
              List(Payment(LocalDate.parse("2021-05-13"), 100))
            )
          ),
          Seq.empty[PaymentInformation],
          Seq.empty[CustomerPostCode],
          Seq(
            Instalment(
              DebtItemChargeId("debtItemChargeId"),
              LocalDate.parse("2021-05-01"),
              amountDue = 100,
              expectedPayment = 100,
              interestRate = 0.26,
              instalmentNumber = 1,
              instalmentInterestAccrued = 10.20,
              instalmentBalance = 100
            )
          ),
          collections = Collections(
            initialCollection = None,
            regularCollections = List(
              RegularCollection(dueDate = LocalDate.parse("2021-05-01"), amountDue = 100),
              RegularCollection(dueDate = LocalDate.parse("2021-06-01"), amountDue = 100)
            )
          )
        )

      "should send the enrolment scope to the authorise endpoint" - {
        "and return a 200" in {
          stubRequest(
            httpMethod = POST,
            url = "/auth/authorise",
            status = 200,
            responseBody = "null",
            requestBodyContaining = Some(
              Json
                .parse(
                  """{"authorise":[{"identifiers":[],"state":"Activated","enrolment":"read:time-to-pay-proxy"}],"retrieve":[]}"""
                )
                .toString()
            )
          )

          stubRequest(
            httpMethod = GET,
            url = "/debts/time-to-pay/quote/customerReference/planId",
            status = 200,
            responseBody = Json.toJson(responsePayload).toString()
          )

          val request: WSRequest = buildRequest("/quote/customerReference/planId")
          val response: WSResponse = await(
            request.get()
          )

          WireMock.verify(1, postRequestedFor(urlPathEqualTo("/auth/authorise")))
          WireMock.verify(1, getRequestedFor(urlPathEqualTo("/debts/time-to-pay/quote/customerReference/planId")))

          response.json.as[ViewPlanResponse] shouldBe responsePayload
          response.status shouldBe 200
        }
      }
    }

    ".updatePlan" - {
      val requestPayload: UpdatePlanRequest =
        UpdatePlanRequest(
          CustomerReference("customerReference"),
          PlanId("planId"),
          UpdateType("updateType"),
          channelIdentifier = None,
          Some(PlanStatus.Success),
          completeReason = None,
          Some(CancellationReason("reason")),
          thirdPartyBank = Some(true),
          payments = Some(
            List(
              PaymentInformation(PaymentMethod.Bacs, Some(PaymentReference("reference")))
            )
          )
        )

      val responsePayload: UpdatePlanResponse =
        UpdatePlanResponse(
          CustomerReference("customerReference"),
          PlanId("planId"),
          PlanStatus.Success,
          planUpdatedDate = LocalDate.now
        )

      "should send the enrolment scope to the authorise endpoint" - {
        "and return a 200" in {
          stubRequest(
            httpMethod = POST,
            url = "/auth/authorise",
            status = 200,
            responseBody = "null",
            requestBodyContaining = Some(
              Json
                .parse(
                  """{"authorise":[{"identifiers":[],"state":"Activated","enrolment":"read:time-to-pay-proxy"}],"retrieve":[]}"""
                )
                .toString()
            )
          )

          stubRequest(
            httpMethod = PUT,
            url = "/debts/time-to-pay/quote/customerReference/planId",
            status = 200,
            responseBody = Json.toJson(responsePayload).toString()
          )

          val request: WSRequest = buildRequest("/quote/customerReference/planId")
          val response: WSResponse = await(
            request.put(Json.toJson(requestPayload))
          )

          WireMock.verify(1, postRequestedFor(urlPathEqualTo("/auth/authorise")))
          WireMock.verify(1, putRequestedFor(urlPathEqualTo("/debts/time-to-pay/quote/customerReference/planId")))

          response.json.as[UpdatePlanResponse] shouldBe responsePayload
          response.status shouldBe 200
        }
      }
    }

    ".createPlan" - {
      val requestPayload: CreatePlanRequest = CreatePlanRequest(
        CustomerReference("customerReference"),
        QuoteReference("quoteReference"),
        ChannelIdentifier.Advisor,
        PlanToCreatePlan(
          QuoteId("quoteId"),
          QuoteType.Duration,
          quoteDate = LocalDate.parse("2010-02-02"),
          instalmentStartDate = LocalDate.parse("2010-02-02"),
          instalmentAmount = Some(100),
          PaymentPlanType.TimeToPay,
          thirdPartyBank = false,
          numberOfInstalments = 2,
          Some(FrequencyLowercase.Single),
          Some(Duration(2)),
          Some(PaymentMethod.Bacs),
          Some(PaymentReference("ref123")),
          initialPaymentDate = Some(LocalDate.parse("2010-02-02")),
          initialPaymentAmount = Some(100),
          totalDebtIncInt = 100,
          totalInterest = 10,
          interestAccrued = 10,
          planInterest = 10
        ),
        List(
          CreatePlanDebtItemCharge(
            DebtItemChargeId("debtItemChargeId"),
            mainTrans = "1525",
            subTrans = "1000",
            originalDebtAmount = 100,
            interestStartDate = Some(LocalDate.parse("2010-02-02")),
            List(Payment(LocalDate.parse("2020-01-01"), 100)),
            dueDate = None,
            chargeSource = None,
            parentChargeReference = None,
            parentMainTrans = None
          )
        ),
        List(PaymentInformation(PaymentMethod.Bacs, Some(PaymentReference("ref123")))),
        List(CustomerPostCode(PostCode("NW1 AB1"), postcodeDate = LocalDate.parse("2010-02-02"))),
        List(
          Instalment(
            DebtItemChargeId("id1"),
            dueDate = LocalDate.parse("2010-02-02"),
            amountDue = 100,
            expectedPayment = 100,
            interestRate = 0.24,
            instalmentNumber = 1,
            instalmentInterestAccrued = 10,
            instalmentBalance = 90
          )
        ),
        regimeType = None
      )

      val responsePayload: CreatePlanResponse =
        CreatePlanResponse(
          CustomerReference("customerReference"),
          PlanId("planId"),
          CaseId("caseId"),
          PlanStatus.Success
        )

      "should send the enrolment scope to the authorise endpoint" - {
        "and return a 200" in {
          stubRequest(
            httpMethod = POST,
            url = "/auth/authorise",
            status = 200,
            responseBody = "null",
            requestBodyContaining = Some(
              Json
                .parse(
                  """{"authorise":[{"identifiers":[],"state":"Activated","enrolment":"read:time-to-pay-proxy"}],"retrieve":[]}"""
                )
                .toString()
            )
          )

          stubRequest(
            httpMethod = POST,
            url = "/debts/time-to-pay/quote/arrangement",
            status = 201,
            responseBody = Json.toJson(responsePayload).toString()
          )

          val request: WSRequest = buildRequest("/quote/arrangement")
          val response: WSResponse = await(
            request.post(Json.toJson(requestPayload))
          )

          WireMock.verify(1, postRequestedFor(urlPathEqualTo("/auth/authorise")))
          WireMock.verify(1, postRequestedFor(urlPathEqualTo("/debts/time-to-pay/quote/arrangement")))

          response.json.as[CreatePlanResponse] shouldBe responsePayload
          response.status shouldBe 200
        }
      }
    }

    ".getAffordableQuotes" - {
      val requestPayload: AffordableQuotesRequest = AffordableQuotesRequest(
        channelIdentifier = "eSSTTP",
        paymentPlanAffordableAmount = 500,
        paymentPlanFrequency = FrequencyCapitalised.Monthly,
        paymentPlanMaxLength = Duration(6),
        paymentPlanMinLength = Duration(1),
        accruedDebtInterest = 500,
        paymentPlanStartDate = LocalDate.parse("2022-02-02"),
        initialPaymentDate = Some(LocalDate.parse("2022-02-02")),
        initialPaymentAmount = Some(500),
        debtItemCharges = List(
          DebtItemChargeSelfServe(
            outstandingDebtAmount = 100000,
            mainTrans = "1525",
            subTrans = "1000",
            DebtItemChargeId("ChargeRef 0903_2"),
            interestStartDate = Some(LocalDate.parse("2021-09-03")),
            debtItemOriginalDueDate = LocalDate.parse("2010-02-02"),
            IsInterestBearingCharge(true),
            UseChargeReference(false)
          )
        ),
        customerPostcodes = List(
          CustomerPostCode(
            PostCode("some postcode"),
            LocalDate.parse("2022-03-09")
          )
        ),
        regimeType = Some(SsttpRegimeType.SA)
      )

      val responsePayload: AffordableQuoteResponse =
        AffordableQuoteResponse(LocalDateTime.parse("2025-01-13T10:15:30.975"), Nil)

      "should send the enrolment scope to the authorise endpoint" - {
        "and return a 200" in {
          stubRequest(
            httpMethod = POST,
            url = "/auth/authorise",
            status = 200,
            responseBody = "null",
            requestBodyContaining = Some(
              Json
                .parse(
                  """{"authorise":[{"identifiers":[],"state":"Activated","enrolment":"read:time-to-pay-proxy"}],"retrieve":[]}"""
                )
                .toString()
            )
          )

          stubRequest(
            httpMethod = POST,
            url = "/debts/time-to-pay/affordability/affordable-quotes",
            status = 200,
            responseBody = Json.toJson(responsePayload).toString()
          )

          val request: WSRequest = buildRequest("/self-serve/affordable-quotes")
          val response: WSResponse = await(
            request.post(Json.toJson(requestPayload))
          )

          WireMock.verify(1, postRequestedFor(urlPathEqualTo("/auth/authorise")))
          WireMock.verify(1, postRequestedFor(urlPathEqualTo("/debts/time-to-pay/affordability/affordable-quotes")))

          response.json.as[AffordableQuoteResponse] shouldBe responsePayload
          response.status shouldBe 200
        }
      }
    }

    ".checkChargeInfo" - {
      val requestPayload: ChargeInfoRequest = ChargeInfoRequest(
        channelIdentifier = ChargeInfoChannelIdentifier("Channel Identifier"),
        identifications = NonEmptyList.of(
          Identification(idType = IdType("id type 1"), idValue = IdValue("id value 1")),
          Identification(idType = IdType("id type 2"), idValue = IdValue("id value 2"))
        ),
        regimeType = SaOnlyRegimeType.SA
      )

      val responsePayload: ChargeInfoResponse = ChargeInfoResponseR2(
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
        ),
        customerSignals = Some(
          List(
            Signal(SignalType("Rls"), SignalValue("signal value"), Some("description")),
            Signal(SignalType("Welsh Language Signal"), SignalValue("signal value"), Some("description"))
          )
        )
      )

      "should send the enrolment scope to the authorise endpoint" - {
        "and return a 200" in {
          val mockFeatureSwitch = mock[FeatureSwitch]
          implicit val chargeInfoResponseReads: Reads[ChargeInfoResponse] = ChargeInfoResponse.reads(mockFeatureSwitch)

          (() => mockFeatureSwitch.saRelease2Enabled)
            .expects()
            .returning(SARelease2Enabled(true))

          stubRequest(
            httpMethod = POST,
            url = "/auth/authorise",
            status = 200,
            responseBody = "null",
            requestBodyContaining = Some(
              Json
                .parse(
                  """{"authorise":[{"identifiers":[],"state":"Activated","enrolment":"read:time-to-pay-proxy"}],"retrieve":[]}"""
                )
                .toString()
            )
          )

          stubRequest(
            httpMethod = POST,
            url = "/debts/time-to-pay/charge-info",
            status = 200,
            responseBody = Json.toJson(responsePayload).toString()
          )

          val request: WSRequest = buildRequest("/charge-info")
          val response: WSResponse = await(
            request.post(Json.toJson(requestPayload))
          )

          WireMock.verify(1, postRequestedFor(urlPathEqualTo("/auth/authorise")))
          WireMock.verify(1, postRequestedFor(urlPathEqualTo("/debts/time-to-pay/charge-info")))

          response.json.as[ChargeInfoResponse] shouldBe responsePayload
          response.status shouldBe 200
        }
      }
    }

    ".cancelTtp" - {
      val requestPayload: TtpCancelRequest =
        TtpCancelRequest(
          identifications = NonEmptyList.of(
            Identification(
              idType = IdType("idtype"),
              idValue = IdValue("idvalue")
            )
          ),
          paymentPlan = TtpCancelPaymentPlan(
            arrangementAgreedDate = ArrangementAgreedDate(LocalDate.parse("2020-01-02")),
            ttpEndDate = TtpEndDate(LocalDate.parse("2020-02-04")),
            frequency = FrequencyLowercase.Weekly,
            cancellationDate = CancellationDate(LocalDate.parse("2020-03-05")),
            initialPaymentDate = Some(InitialPaymentDate(LocalDate.parse("2020-04-06"))),
            initialPaymentAmount = Some(GbpPounds.createOrThrow(100.12))
          ),
          instalments = NonEmptyList.of(
            SaOnlyInstalment(
              dueDate = InstalmentDueDate(LocalDate.parse("2020-05-07")),
              amountDue = GbpPounds.createOrThrow(200.34)
            )
          ),
          channelIdentifier = ChannelIdentifier.SelfService,
          transitioned = Some(TransitionedIndicator(true))
        )

      val responsePayload: TtpCancelSuccessfulResponse =
        TtpCancelSuccessfulResponse(
          apisCalled = List(
            ApiStatus(
              name = ApiName("api name"),
              statusCode = ApiStatusCode(400),
              processingDateTime = ProcessingDateTimeInstant(Instant.parse("2000-01-02T14:35:00.788998Z")),
              errorResponse = Some(ApiErrorResponse("api error response"))
            )
          ),
          processingDateTime = ProcessingDateTimeInstant(Instant.parse("2222-02-24T14:35:00.788998Z"))
        )

      "should send the enrolment scope to the authorise endpoint" - {
        "and return a 200" in {
          stubRequest(
            httpMethod = POST,
            url = "/auth/authorise",
            status = 200,
            responseBody = "null",
            requestBodyContaining = Some(
              Json
                .parse(
                  """{"authorise":[{"identifiers":[],"state":"Activated","enrolment":"read:time-to-pay-proxy"}],"retrieve":[]}"""
                )
                .toString()
            )
          )

          stubRequest(
            httpMethod = POST,
            url = "/debts/time-to-pay/cancel",
            status = 200,
            responseBody = Json.toJson(responsePayload).toString()
          )

          val request: WSRequest = buildRequest("/cancel")
          val response: WSResponse = await(
            request.post(Json.toJson(requestPayload))
          )

          WireMock.verify(1, postRequestedFor(urlPathEqualTo("/auth/authorise")))
          WireMock.verify(1, postRequestedFor(urlPathEqualTo("/debts/time-to-pay/cancel")))

          response.json.as[TtpCancelSuccessfulResponse] shouldBe responsePayload
          response.status shouldBe 200
        }
      }
    }

    ".informTtp" - {
      val requestPayload: TtpInformRequest =
        TtpInformRequest(
          identifications = NonEmptyList.of(
            Identification(
              idType = IdType("idtype"),
              idValue = IdValue("idvalue")
            )
          ),
          paymentPlan = SaOnlyPaymentPlan(
            arrangementAgreedDate = ArrangementAgreedDate(LocalDate.parse("2020-01-02")),
            ttpEndDate = TtpEndDate(LocalDate.parse("2020-02-04")),
            frequency = FrequencyLowercase.Weekly,
            initialPaymentDate = Some(InitialPaymentDate(LocalDate.parse("2020-04-06"))),
            initialPaymentAmount = Some(GbpPounds.createOrThrow(100.12)),
            ddiReference = Some(DdiReference("TestDDIReference"))
          ),
          instalments = NonEmptyList.of(
            SaOnlyInstalment(
              dueDate = InstalmentDueDate(LocalDate.parse("2020-05-07")),
              amountDue = GbpPounds.createOrThrow(200.34)
            )
          ),
          channelIdentifier = ChannelIdentifier.SelfService,
          transitioned = Some(TransitionedIndicator(true))
        )

      val responsePayload: TtpInformSuccessfulResponse =
        TtpInformSuccessfulResponse(
          apisCalled = List(
            ApiStatus(
              name = ApiName("api name"),
              statusCode = ApiStatusCode(400),
              processingDateTime = ProcessingDateTimeInstant(Instant.parse("2000-01-02T14:35:00.788998Z")),
              errorResponse = Some(ApiErrorResponse("api error response"))
            )
          ),
          processingDateTime = ProcessingDateTimeInstant(Instant.parse("2222-02-24T14:35:00.788998Z"))
        )

      "should send the enrolment scope to the authorise endpoint" - {
        "and return a 200" in {
          stubRequest(
            httpMethod = POST,
            url = "/auth/authorise",
            status = 200,
            responseBody = "null",
            requestBodyContaining = Some(
              Json
                .parse(
                  """{"authorise":[{"identifiers":[],"state":"Activated","enrolment":"read:time-to-pay-proxy"}],"retrieve":[]}"""
                )
                .toString()
            )
          )

          stubRequest(
            httpMethod = POST,
            url = "/debts/time-to-pay/inform",
            status = 200,
            responseBody = Json.toJson(responsePayload).toString()
          )

          val request: WSRequest = buildRequest("/inform")
          val response: WSResponse = await(
            request.post(Json.toJson(requestPayload))
          )

          WireMock.verify(1, postRequestedFor(urlPathEqualTo("/auth/authorise")))
          WireMock.verify(1, postRequestedFor(urlPathEqualTo("/debts/time-to-pay/inform")))

          response.json.as[TtpInformSuccessfulResponse] shouldBe responsePayload
          response.status shouldBe 200
        }
      }
    }

    ".fullAmendTtp" - {
      val requestPayload: TtpFullAmendRequestR1 =
        TtpFullAmendRequestR1(
          identifications = NonEmptyList.of(
            Identification(
              idType = IdType("idtype"),
              idValue = IdValue("idvalue")
            )
          ),
          paymentPlan = SaOnlyPaymentPlan(
            arrangementAgreedDate = ArrangementAgreedDate(LocalDate.parse("2020-01-02")),
            ttpEndDate = TtpEndDate(LocalDate.parse("2020-02-04")),
            frequency = FrequencyLowercase.Weekly,
            initialPaymentDate = Some(InitialPaymentDate(LocalDate.parse("2020-04-06"))),
            initialPaymentAmount = Some(GbpPounds.createOrThrow(100.12)),
            ddiReference = Some(DdiReference("TestDDIReference"))
          ),
          instalments = NonEmptyList.of(
            SaOnlyInstalment(
              dueDate = InstalmentDueDate(LocalDate.parse("2020-05-07")),
              amountDue = GbpPounds.createOrThrow(200.34)
            )
          ),
          channelIdentifier = ChannelIdentifier.SelfService,
          transitioned = TransitionedIndicator(true)
        )

      val responsePayload: TtpFullAmendSuccessfulResponse =
        TtpFullAmendSuccessfulResponse(
          apisCalled = List(
            ApiStatus(
              name = ApiName("API1"),
              statusCode = ApiStatusCode(200),
              processingDateTime = ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z")),
              errorResponse = None
            )
          ),
          processingDateTime = ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z"))
        )

      "should send the enrolment scope to the authorise endpoint" - {
        "and return a 200" in {
          stubRequest(
            httpMethod = POST,
            url = "/auth/authorise",
            status = 200,
            responseBody = "null",
            requestBodyContaining = Some(
              Json
                .parse(
                  """{"authorise":[{"identifiers":[],"state":"Activated","enrolment":"read:time-to-pay-proxy"}],"retrieve":[]}"""
                )
                .toString()
            )
          )

          stubRequest(
            httpMethod = POST,
            url = "/debts/time-to-pay/full-amend",
            status = 200,
            responseBody = Json.toJson(responsePayload).toString()
          )

          val request: WSRequest = buildRequest("/full-amend")
          val response: WSResponse = await(
            request.post(Json.toJson(requestPayload))
          )

          WireMock.verify(1, postRequestedFor(urlPathEqualTo("/auth/authorise")))
          WireMock.verify(1, postRequestedFor(urlPathEqualTo("/debts/time-to-pay/full-amend")))

          response.json.as[TtpFullAmendSuccessfulResponse] shouldBe responsePayload
          response.status shouldBe 200
        }
      }
    }
  }
}
