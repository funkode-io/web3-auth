/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.input
package adapters

import zio.*
import zio.http.*
import zio.http.model.Method
import zio.test.*
import zio.test.Assertion.*

import io.funkode.web3.auth.model.*

trait WalletAndChallengeExamples:

  val walletAddress1 = "0xef678007D18427E6022059Dbc264f27507CD1ffC"
  val wallet1 = Wallet(WalletAddress("0xef678007D18427E6022059Dbc264f27507CD1ffC"))
  val challengeMessage1 = Message("challenge1")
  val signature1 = Signature("signature1")
  val token1 = Token("token1")
  val claims1 = Claims(Subject(walletAddress1), None)
  val wallet2 = Wallet(WalletAddress("0x31b26E43651e9371C88aF3D36c14CfD938BaF4Fd"))
  val wallet3 = Wallet(WalletAddress("0x44A84615dD457f729bbbf85f009F3d2e8d484D91"))

class MockAuthenticationService extends AuthenticationService with WalletAndChallengeExamples:
  def createChallengeMessage(wallet: Wallet): AuthIO[Message] =
    if wallet == wallet1 then ZIO.succeed(challengeMessage1)
    else ZIO.fail(AuthenticationError.InvalidWallet(wallet))

  def login(wallet: Wallet, challenge: Message, sign: Signature): AuthIO[Token] =
    if (wallet == wallet1 && challenge == challengeMessage1 && sign == signature1) then ZIO.succeed(token1)
    else ZIO.fail(AuthenticationError.BadCredentials("bad signature"))

  def validateToken(token: Token): AuthIO[Claims] =
    if (token == token1) then ZIO.succeed(claims1)
    else ZIO.fail(AuthenticationError.InvalidToken(token, new Throwable("wrong token")))

object RestAuthenticationApiSpec extends ZIOSpecDefault with WalletAndChallengeExamples:

  import RestAuthenticationApi.app

  val mockService = ZLayer(ZIO.succeed(new MockAuthenticationService()))

  def spec = suite("Rest Authentication API should")(
    test("create challenge for a wallet") {
      val request = Request.post(Body.empty, URL(!! / "challenge" / walletAddress1))
      assertZIO(app.runZIO(request))(equalTo(Response.text(challengeMessage1.unwrap)))
    },
    test("create a token if signature matches challenge") {
      val requestJson = s"""{"message": "$challengeMessage1", "signature": "$signature1"}"""
      val request = Request.post(Body.fromString(requestJson), URL(!! / "login" / walletAddress1))
      assertZIO(app.runZIO(request))(equalTo(Response.text(token1.unwrap)))
    },
    test("get claims from proper token") {
      val request = Request.default(Method.GET, URL(!! / "claims"), Body.fromString(token1.unwrap))
      val responseJson = s"""{"sub":"${claims1.sub}"}"""
      assertZIO(app.runZIO(request))(equalTo(Response.text(responseJson)))
    }
  ).provideShared(mockService)
