/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.input
package adapters

import io.funkode.resource.model.*
import io.lemonlabs.uri.Url
import zio.*
import zio.http.*
import zio.http.model.*
import zio.json.*

import io.funkode.web3.auth.model.*

object RestAuthenticationApi:

  case class CreateChallenge(walletAddress: String) derives JsonCodec
  case class LoginRequest(walletAddress: String, signature: String) derives JsonCodec

  extension (request: Request)
    def parsedAs[R: JsonDecoder] = JsonDecoder[R]
      .decodeJsonStreamInput(request.body.asStream)
      .mapError(e => Response.fromHttpError(HttpError.BadRequest(s"Error decoding request: ${e.getMessage}")))

    def remoteAddressString = request.remoteAddress.map(_.toString).getOrElse("")

  val app = Http.collectZIO[Request] {
    case req @ Method.POST -> !! / "login" =>
      for
        createChallengeRequest <- req.parsedAs[CreateChallenge]
        challenge <- AuthenticationService
          .createChallengeMessage(createChallengeRequest.walletAddress)
          .flatMapError(mapErrorToResponse)
      yield Response
        .text(challenge.unwrap)
        .withLocation(req.remoteAddressString + "/login/" + challenge.unwrap)

    case req @ Method.POST -> !! / "login" / challenge =>
      for
        loginRequest <- JsonDecoder[LoginRequest]
          .decodeJsonStreamInput(req.body.asStream)
          .mapError(e =>
            Response.fromHttpError(HttpError.BadRequest(s"Error decoding login request: ${e.getMessage}"))
          )
        LoginRequest(walletAddress, signature) = loginRequest
        token <- AuthenticationService
          .login(walletAddress, Message(challenge), Signature(signature))
          .flatMapError(mapErrorToResponse)
      yield Response.text(token.unwrap).withLocation("/claims/" + token.unwrap)

    case Method.GET -> !! / "claims" / token =>
      for claims <- AuthenticationService
          .validateToken(Token(token))
          .flatMapError(mapErrorToResponse)
      yield Response.text(claims.toJson).withLocation("/claims/" + token)
  }

  given Conversion[String, Wallet] = publicAddress => Wallet(WalletAddress(publicAddress))

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
