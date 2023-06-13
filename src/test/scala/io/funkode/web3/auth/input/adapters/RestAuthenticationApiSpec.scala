/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.input
package adapters

import io.funkode.resource.model.*
import io.lemonlabs.uri.Urn
import zio.*
import zio.http.*
import zio.test.*
import zio.test.Assertion.*

import io.funkode.web3.auth.model.*

trait WalletAndChallengeExamples:

  val walletAddress1 = "0xef678007D18427E6022059Dbc264f27507CD1ffC"
  val wallet1 = Wallet(WalletAddress("0xef678007D18427E6022059Dbc264f27507CD1ffC"))

  val challenge1Uuid = java.util.UUID.fromString("e58ed763-928c-4155-bee9-fdbaaadc15f3")
  val challenge1Message = Message("Challenge:\ne58ed763-928c-4155-bee9-fdbaaadc15f3")
  val challenge1 = Challenge(challenge1Uuid, challenge1Message, -1L)

  val signature1 = Signature("signature1")
  val token1 = Token("token1")
  val claims1 = Claims(Subject(walletAddress1), None)
  val wallet2 = Wallet(WalletAddress("0x31b26E43651e9371C88aF3D36c14CfD938BaF4Fd"))
  val wallet3 = Wallet(WalletAddress("0x44A84615dD457f729bbbf85f009F3d2e8d484D91"))

class MockAuthenticationService extends AuthenticationService with WalletAndChallengeExamples:
  def createLoginChallenge(wallet: Wallet): AuthIO[Resource.Of[Challenge]] =
    if wallet == wallet1 then ZIO.succeed(Resource.fromAddressableClass(challenge1))
    else ZIO.fail(AuthenticationError.InvalidWallet(wallet))

  def login(challengeUrn: Urn, sign: Signature): AuthIO[Token] =
    if challengeUrn == challenge1.urn && sign == signature1 then ZIO.succeed(token1)
    else ZIO.fail(AuthenticationError.BadCredentials("bad signature"))

  def validateToken(token: Token): AuthIO[Claims] =
    if token == token1 then ZIO.succeed(claims1)
    else ZIO.fail(AuthenticationError.InvalidToken(token, new Throwable("wrong token")))

object RestAuthenticationApiSpec extends ZIOSpecDefault with WalletAndChallengeExamples:

  import RestAuthenticationApi.app

  val mockService = ZLayer(ZIO.succeed(new MockAuthenticationService()))

  def spec = suite("Rest Authentication API should")(
    test("create challenge for a wallet") {
      val createChallengeJson = s"""{"walletAddress": "$walletAddress1"}"""
      val request = Request.post(Body.fromString(createChallengeJson), URL(!! / "login"))

      assertZIO(app.runZIO(request))(
        equalTo(
          Response.text(challenge1.message.unwrap).withLocation("/login/" + challenge1.uuid.toString)
        )
      )
    },
    test("create a token if signature matches challenge") {
      val request =
        Request.post(Body.fromString(signature1.unwrap), URL(!! / "login" / challenge1.uuid.toString))
      assertZIO(app.runZIO(request))(
        equalTo(Response.text(token1.unwrap).withLocation("/claims/" + token1.unwrap))
      )
    },
    test("get claims from proper token") {
      val request = Request.get(URL(!! / "claims" / token1.unwrap))
      val responseJson = s"""{"sub":"${claims1.sub}"}"""
      assertZIO(app.runZIO(request))(
        equalTo(Response.text(responseJson).withLocation("/claims/" + token1.unwrap))
      )
    }
  ).provideShared(mockService)
