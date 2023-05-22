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
import zio.http.model.*
import zio.json.*

import io.funkode.web3.auth.model.*

object RestAuthenticationApi:

  case class LoginRequest(
      message: String,
      signature: String
  ) derives JsonCodec

  def showCause(cause: Option[Throwable]): String =
    cause.map(c => s": " + c.getMessage).getOrElse("")

  def mapErrorToResponse(authenticationError: AuthenticationError): URIO[AuthenticationService, Response] =
    authenticationError match
      case e @ AuthenticationError.InvalidWallet(invalidWallet, cause) =>
        ZIO.logErrorCause(s"Invalid wallet${showCause(cause)}", Cause.fail(e)) *>
          ZIO.succeed(Response.fromHttpError(HttpError.BadRequest(s"Invalid wallet: $invalidWallet")))
      case e @ AuthenticationError.BadCredentials(message, cause) =>
        ZIO.logErrorCause(s"Invalid credentials${showCause(cause)}", Cause.fail(e)) *>
          ZIO.succeed(Response.fromHttpError(HttpError.Unauthorized(s"Invalid credentials: $message")))
      case e @ AuthenticationError.InvalidToken(token, cause) =>
        ZIO.logErrorCause(s"Invalid token${cause.getMessage}", Cause.fail(e)) *>
          ZIO.succeed(Response.fromHttpError(HttpError.Unauthorized(s"Invalid token: $token")))
      case e @ AuthenticationError.Internal(msg, cause) =>
        ZIO.logErrorCause(s"Internal server error${cause.getMessage}", Cause.fail(e)) *>
          ZIO.succeed(Response.fromHttpError(HttpError.InternalServerError(s"Invalid server error: $msg")))

  val app = Http.collectZIO[Request] {
    case Method.POST -> !! / "challenge" / publicAddress =>
      AuthenticationService
        .createChallengeMessage(publicAddress)
        .map(_.unwrap)
        .map(Response.text)
        .flatMapError(mapErrorToResponse)

    case req @ Method.POST -> !! / "login" / publicAddress =>
      for
        loginRequest <- JsonDecoder[LoginRequest]
          .decodeJsonStreamInput(req.body.asStream)
          .mapError(e =>
            Response.fromHttpError(HttpError.BadRequest(s"Error decoding login request: ${e.getMessage}"))
          )
        LoginRequest(message, signature) = loginRequest
        token <- AuthenticationService
          .login(publicAddress, Message(message), Signature(signature))
          .flatMapError(mapErrorToResponse)
      yield Response.text(token.unwrap)

    case req @ Method.GET -> !! / "claims" =>
      for
        token <- req.body.asString.mapError(e =>
          Response.fromHttpError(HttpError.BadRequest(s"Error reading token from request: ${e.getMessage}"))
        )
        claims <- AuthenticationService
          .validateToken(Token(token))
          .flatMapError(mapErrorToResponse)
      yield Response.text(claims.toJson)
  }

  given Conversion[String, Wallet] = publicAddress => Wallet(WalletAddress(publicAddress))
