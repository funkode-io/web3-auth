/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.input
package adapters

import io.funkode.resource.model.*
import zio.*
import zio.http.*
import zio.http.model.Method
import zio.json.*

import io.funkode.web3.auth.model.*

object RestAuthenticationApi:

  case class LoginRequest(
      message: String,
      signature: String
  ) derives JsonCodec

  val app = Http.collectZIO[Request] {
    case Method.POST -> !! / "challenge" / publicAddress =>
      AuthenticationService.createChallengeMessage(publicAddress).map(_.unwrap).map(Response.text)

    case req @ Method.POST -> !! / "login" / publicAddress =>
      for
        loginRequest <- JsonDecoder[LoginRequest].decodeJsonStreamInput(req.body.asStream)
        LoginRequest(message, signature) = loginRequest
        token <- AuthenticationService.login(publicAddress, Message(message), Signature(signature))
      yield Response.text(token.unwrap)

    case req @ Method.GET -> !! / "claims" =>
      for
        token <- req.body.asString
        claims <- AuthenticationService.validateToken(Token(token))
      yield Response.text(claims.toJson)

  }

  given Conversion[String, Wallet] = publicAddress => Wallet(WalletAddress(publicAddress))
