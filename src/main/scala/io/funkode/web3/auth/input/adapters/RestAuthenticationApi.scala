/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.input
package adapters

import io.lemonlabs.uri.Urn
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
      .tapError(e => ZIO.logErrorCause(s"Error parsing request ${request.url}", Cause.fail(e)))
      .mapError(e => Response.fromHttpError(HttpError.BadRequest(s"Error decoding request: ${e.getMessage}")))

  val app = Http.collectZIO[Request]:
    case req @ Method.POST -> !! / "login" =>
      for
        createChallengeRequest <- req.parsedAs[CreateChallenge]
        challengeResource <- AuthenticationService
          .createLoginChallenge(createChallengeRequest.walletAddress)
          .flatMapError(mapErrorToResponse)
        challenge <- challengeResource.body.mapError(e =>
          Response.fromHttpError(
            HttpError.InternalServerError(s"Internal error parsing challenge: ${e.getMessage}")
          )
        )
      yield Response
        .text(challenge.message.unwrap)
        .withLocation("/login/" + challengeResource.urn.nss)

    case req @ Method.POST -> !! / "login" / challengeUuid =>
      for
        signature <- req.body.asString.mapError(e =>
          Response.fromHttpError(HttpError.BadRequest(s"Error reading signature : ${e.getMessage}"))
        )
        token <- AuthenticationService
          .login(Urn(Challenge.Nid, challengeUuid), Signature(signature))
          .flatMapError(mapErrorToResponse)
      yield Response.text(token.unwrap).withLocation("/claims/" + token.unwrap)

    case Method.GET -> !! / "claims" / token =>
      for claims <- AuthenticationService
          .validateToken(Token(token))
          .flatMapError(mapErrorToResponse)
      yield Response.text(claims.toJson).withLocation("/claims/" + token)

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
