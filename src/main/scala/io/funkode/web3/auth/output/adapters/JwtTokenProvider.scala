/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.output
package adapters

import pdi.jwt.*
import zio.*

import io.funkode.web3.auth.model.*

class JwtTokenProvider(config: JwtConfig) extends TokenService:
  def createToken(subject: Subject): TokenIO[Token] =
    ZIO.succeed(Token(Jwt.encode(s"""{"sub": "${subject.unwrap}"}""", config.secret, JwtAlgorithm.HS256)))

  def getClaims(token: Token): TokenIO[Claims] =
    for
      decoded <- ZIO
        .fromTry(Jwt.decode(token.unwrap, config.secret, Seq(JwtAlgorithm.HS256)))
        .mapError(e => TokenError.WrongTokenFormat("Error parsing token", e))
      subject <- ZIO.fromOption(decoded.subject).orElseFail(TokenError.MissingSubject)
    yield Claims(Subject(subject), decoded.issuedAt)

object JwtTokenProvider:

  val live: ZLayer[JwtConfig, Nothing, TokenService] =
    ZLayer(ZIO.service[JwtConfig].map(config => new JwtTokenProvider(config)))
