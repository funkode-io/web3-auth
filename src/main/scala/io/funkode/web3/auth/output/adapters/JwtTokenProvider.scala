/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.output
package adapters

import java.time.Instant

import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtZIOJson}
import zio.*

import io.funkode.web3.auth.model.*

class JwtTokenProvider(config: JwtConfig) extends TokenService:

  def createToken(subject: Subject): TokenIO[Token] =
    val claim = JwtClaim(
      subject = Some(subject.toString),
      issuedAt = Some(Instant.now.getEpochSecond)
    )
    ZIO.succeed(Token(JwtZIOJson.encode(claim, config.signingKey, JwtAlgorithm.HS256)))

  def parseToken(token: Token): TokenIO[Claims] =
    for
      decoded <- ZIO
        .fromTry(JwtZIOJson.decode(token.unwrap, config.signingKey, Seq(JwtAlgorithm.HS256)))
        .mapError(e => TokenError.WrongTokenFormat(token, e))
      subject <- ZIO.fromOption(decoded.subject).orElseFail(TokenError.MissingSubject(token))
    yield Claims(Subject(subject), decoded.issuedAt)

object JwtTokenProvider:

  val live: ZLayer[JwtConfig, Nothing, TokenService] =
    ZLayer(ZIO.service[JwtConfig].map(config => new JwtTokenProvider(config)))
