/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.model

import zio.json.*

enum TokenType:
  case Jwt

case class Token(value: String, tokenType: TokenType)
object Token:

  def apply(value: String): Token = Token(value, TokenType.Jwt)
  extension (token: Token) def unwrap: String = token.value

opaque type Subject = String
object Subject:
  def apply(value: String): Subject = value
  extension (x: Subject) def unwrap: String = x

  given JsonCodec[Subject] =
    JsonCodec(JsonEncoder[String].contramap(unwrap), JsonDecoder[String].map(Subject.apply))

case class Claims(sub: Subject, iat: Option[Long]) derives JsonCodec
