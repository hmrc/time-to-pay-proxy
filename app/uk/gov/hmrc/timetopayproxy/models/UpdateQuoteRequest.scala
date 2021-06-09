/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.timetopayproxy.models

import play.api.libs.json.Json

final case class CancellationReason(value: String) extends AnyVal

object CancellationReason extends ValueTypeFormatter {
  implicit val format =
    valueTypeFormatter(CancellationReason.apply, CancellationReason.unapply)
}

final case class PaymentMethod(value: String) extends AnyVal

object PaymentMethod extends ValueTypeFormatter {
  implicit val format =
    valueTypeFormatter(PaymentMethod.apply, PaymentMethod.unapply)
}

final case class PaymentReference(value: String) extends AnyVal

object PaymentReference extends ValueTypeFormatter {
  implicit val format =
    valueTypeFormatter(PaymentReference.apply, PaymentReference.unapply)
}

final case class UpdateType(value: String) extends AnyVal

object UpdateType extends ValueTypeFormatter {
  implicit val format =
    valueTypeFormatter(UpdateType.apply, UpdateType.unapply)
}

final case class UpdateQuoteRequest(customerReference: CustomerReference,
                                    planId: PlanId,
                                    updateType: UpdateType,
                                    cancellationReason: CancellationReason,
                                    paymentMethod: PaymentMethod,
                                    paymentReference: PaymentReference,
                                    thirdPartyBank: Boolean,
)

object UpdateQuoteRequest {
  implicit val format = Json.format[UpdateQuoteRequest]
}
