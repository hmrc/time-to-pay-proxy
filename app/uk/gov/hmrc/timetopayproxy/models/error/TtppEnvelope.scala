/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.timetopayproxy.models.error

import cats.data.EitherT

import scala.concurrent.{ ExecutionContext, Future }

object TtppEnvelope {
  type TtppEnvelope[T] = EitherT[Future, ProxyEnvelopeError, T]

  def apply[T](arg: T)(implicit ec: ExecutionContext): TtppEnvelope[T] = EitherT.pure[Future, ProxyEnvelopeError](arg)

  def apply[T](f: Future[Either[ProxyEnvelopeError, T]]): TtppEnvelope[T] = EitherT(f)

  def apply[T](eitherArg: Either[ProxyEnvelopeError, T])(implicit ec: ExecutionContext): TtppEnvelope[T] =
    EitherT.fromEither[Future](eitherArg)
}
