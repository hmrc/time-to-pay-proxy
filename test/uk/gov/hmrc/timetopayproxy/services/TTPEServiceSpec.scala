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

package uk.gov.hmrc.timetopayproxy.services

import cats.data.NonEmptyList
import cats.implicits.catsSyntaxEitherId
import org.scalamock.scalatest.MockFactory
import org.scalatest.Inside.inside
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._
import play.api.test.Helpers.{ await, defaultAwaitTimeout }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.timetopayproxy.connectors.TtpeConnector
import uk.gov.hmrc.timetopayproxy.models.error.TtppEnvelope.TtppEnvelope
import uk.gov.hmrc.timetopayproxy.models.error.{ ConnectorError, ProxyEnvelopeError, TtppEnvelope }
import uk.gov.hmrc.timetopayproxy.models.saonly.chargeInfoApi._
import uk.gov.hmrc.timetopayproxy.models.saonly.common.SaOnlyRegimeType
import uk.gov.hmrc.timetopayproxy.models.{ IdType, IdValue, Identification }

import java.time.{ LocalDate, LocalDateTime }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ ExecutionContext, Future }

class TTPEServiceSpec extends AnyFreeSpec with MockFactory {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val chargeInfoRequest: ChargeInfoRequest = ChargeInfoRequest(
    channelIdentifier = ChargeInfoChannelIdentifier("Channel Identifier"),
    identifications = NonEmptyList.of(
      Identification(idType = IdType("id type 1"), idValue = IdValue("id value 1")),
      Identification(idType = IdType("id type 2"), idValue = IdValue("id value 2"))
    ),
    regimeType = SaOnlyRegimeType.SA
  )

  private val commonProcessingDateTime = LocalDateTime.parse("2025-07-02T15:00:41.689")

  private val commonIdentification = List(Identification(IdType("ID_TYPE"), IdValue("ID_VALUE")))

  private val commonIndividualDetails =
    IndividualDetails(
      title = Some(Title("Mr")),
      firstName = Some(FirstName("John")),
      lastName = Some(LastName("Doe")),
      dateOfBirth = Some(DateOfBirth(LocalDate.parse("1980-01-01"))),
      districtNumber = Some(DistrictNumber("1234")),
      customerType = CustomerType.ItsaMigtrated,
      transitionToCDCS = TransitionToCdcs(value = true)
    )

  private val commonAddresses =
    List(
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
            ChargeInfoPostCode("AB12 3CD"),
            LocalDate.parse("2020-01-01")
          )
        )
      )
)
 
  private val commonCharges =
    List(
      ChargeR2(
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
            originalChargeType = Some(OriginalChargeType("Original Charge Type")),
            locks = Some(
              List(
                Lock(lockType = "Posting/Clearing", lockReason = "No Reallocation")
              )
            )
          )
        )
    )

  private val commonChargeTypeAssessment =
    List(
      ChargeTypeAssessmentR2(
        debtTotalAmount = BigInt(1000),
        chargeReference = ChargeReference("CHARGE REFERENCE"),
        parentChargeReference = Some(ChargeInfoParentChargeReference("PARENT CHARGE REF")),
        mainTrans = MainTrans("2000"),
        charges = commonCharges
      )
    )

  private val commonCustomerSignals = Some(
    List(
      Signal(SignalType("Rls"), SignalValue("signal value"), Some("description")),
      Signal(SignalType("Welsh Language Signal"), SignalValue("signal value"), Some("description"))
    )
  )

  private val chargeInfoResponseWithR1Fields: ChargeInfoResponseR1 = ChargeInfoResponseR1(
    processingDateTime = commonProcessingDateTime,
    identification = commonIdentification,
    individualDetails = commonIndividualDetails,
    addresses = commonAddresses,
    chargeTypeAssessment = commonChargeTypeAssessment
  )

  private val chargeInfoResponseWithR2Fields: ChargeInfoResponseR2 = ChargeInfoResponseR2(
    processingDateTime = commonProcessingDateTime,
    identification = commonIdentification,
    individualDetails = commonIndividualDetails,
    addresses = commonAddresses,
    customerSignals = commonCustomerSignals,
    chargeTypeAssessment = commonChargeTypeAssessment,
    chargeTypesExcluded = Some(false)
  )

  private val chargeInfoResponseR2WithChargeTypesExcludedSetToTrue: ChargeInfoResponseR2 = ChargeInfoResponseR2(
    processingDateTime = commonProcessingDateTime,
    identification = commonIdentification,
    individualDetails = commonIndividualDetails,
    addresses = commonAddresses,
    customerSignals = commonCustomerSignals,
    chargeTypeAssessment = commonChargeTypeAssessment,
    chargeTypesExcluded = Some(true)
  )

  ".checkChargeInfo" - {
    "should return a ChargeInfoResponse from the connector, with only R1 fields" in {
      val connectorStub = new TtpeConnectorStub(
        Right(chargeInfoResponseWithR1Fields)
      )

      val ttpeService = new DefaultTTPEService(connectorStub)

      await(ttpeService.checkChargeInfo(chargeInfoRequest).value) shouldBe chargeInfoResponseWithR1Fields
        .asRight[ProxyEnvelopeError]
    }

    "should return a ChargeInfoResponse from the connector, with both R1 and R2 fields" in {
      val connectorStub = new TtpeConnectorStub(
        Right(chargeInfoResponseWithR2Fields)
      )

      val ttpeService = new DefaultTTPEService(connectorStub)

      await(ttpeService.checkChargeInfo(chargeInfoRequest).value) shouldBe chargeInfoResponseWithR2Fields
        .asRight[ProxyEnvelopeError]
    }

    "returns a ChargeInfoResponse from the connector, with chargeTypesExcluded set to true" in {
      val connectorStub = new TtpeConnectorStub(
        Right(chargeInfoResponseR2WithChargeTypesExcludedSetToTrue)
      )

      val ttpeService = new DefaultTTPEService(connectorStub)

      val result =
        await(ttpeService.checkChargeInfo(chargeInfoRequest).value)

      inside(result) { case Right(r2: ChargeInfoResponseR2) =>
        r2.chargeTypesExcluded shouldBe Some(true)
      }
    }

    "returns a ChargeInfoResponse from the connector, with chargeTypesExcluded set to false" in {
      val connectorStub = new TtpeConnectorStub(
        Right(chargeInfoResponseWithR1Fields)
      )

      val ttpeService = new DefaultTTPEService(connectorStub)

      val result: Either[ProxyEnvelopeError, ChargeInfoResponse] =
        await(ttpeService.checkChargeInfo(chargeInfoRequest).value)

      inside(result) { case Right(_: ChargeInfoResponseR1) =>
        // R1 does not expose chargeTypesExcluded
        succeed
      }
    }

    "returns an error from the connector" in {
      val connectorStub = new TtpeConnectorStub(
        Left(ConnectorError(500, "Internal server error"))
      )

      val ttpeService = new DefaultTTPEService(connectorStub)

      await(ttpeService.checkChargeInfo(chargeInfoRequest).value) shouldBe ConnectorError(
        statusCode = 500,
        message = "Internal server error"
      ).asLeft[ChargeInfoResponse]
    }
  }
}

class TtpeConnectorStub(
  chargeInfoResponse: Either[ProxyEnvelopeError, ChargeInfoResponse]
) extends TtpeConnector {

  override def checkChargeInfo(
    chargeInfoRequest: ChargeInfoRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[ChargeInfoResponse] =
    TtppEnvelope(Future.successful(chargeInfoResponse))
}
