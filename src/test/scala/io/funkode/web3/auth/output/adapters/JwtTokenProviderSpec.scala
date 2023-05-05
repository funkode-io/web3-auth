/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.output.adapters

import pdi.jwt.*
import zio.*
import zio.test.*
import zio.test.Assertion.*

import io.funkode.web3.auth.model.*
import io.funkode.web3.auth.output.TokenService

trait TokenExamples:

  val subject1 = Subject("0xef678007d18427e6022059dbc264f27507cd1ffc")

object JwtTokenProviderSpec extends ZIOSpecDefault with TokenExamples:

  override def spec: Spec[TestEnvironment, Any] =
    suite("Jwt Token provider should")(
      test("Create token from subject and get back from claims") {
        for
          token <- TokenService.createToken(subject1)
          claims <- TokenService.parseToken(token)
        yield assertTrue(claims.sub == subject1)
      },
      test("Raise error if token format is invalid") {

        val wrongToken = Token("someRandomText")
        for tokenError <- TokenService.parseToken(wrongToken).flip
        yield assert(tokenError)(
          isSubtype[TokenError.WrongTokenFormat](hasField("token", _.token, equalTo(wrongToken)))
        )
      },
      test("Raise error if token subject is missing") {
        for
          config <- ZIO.service[JwtConfig]
          tokenWithoutSub = Token(
            JwtZIOJson.encode(JwtClaim(content = "{}"), config.signingKey, JwtAlgorithm.HS256)
          )
          tokenError <- TokenService.parseToken(tokenWithoutSub).flip
        yield assert(tokenError)(
          isSubtype[TokenError.MissingSubject](hasField("token", _.token, equalTo(tokenWithoutSub)))
        )
      }
    ).provideShared(
      JwtConfig.default,
      JwtTokenProvider.live
    )
