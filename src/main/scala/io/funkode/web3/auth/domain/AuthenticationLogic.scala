/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth
package domain

import java.util.UUID
import java.util.concurrent.TimeUnit

import io.funkode.resource.model.*
import io.lemonlabs.uri.Urn
import zio.*

import io.funkode.web3.auth.input.*
import io.funkode.web3.auth.model.*
import io.funkode.web3.auth.output.*

class AuthenticationLogic(
    config: AuthenticationServiceConfig,
    store: AuthStore,
    web3: Web3Service,
    tokenService: TokenService
) extends AuthenticationService:

  import AuthStore.ifNotFound

  def createLoginChallenge(wallet: Wallet): AuthIO[Resource.Of[Challenge]] =
    for
      _ <- validateWallet(wallet)
      challenge <- store
        .getChallengeForWallet(wallet)
        .ifNotFound(createAndRegisterChallengeForWallet(wallet))
        .mapStoreErrors
    yield challenge

  def login(challengeUrn: Urn, signature: Signature): AuthIO[Token] =
    for
      challenge <- store.getChallengeByUrn(challengeUrn).mapStoreErrors
      wallet <- store.getWalletForChallenge(challenge).mapStoreErrors
      _ <- web3
        .validateSignature(wallet, challenge.message, signature)
        .mapError(e => AuthenticationError.BadCredentials("wrong signature", Some(e)))
      // _ <- validateChallenge(wallet, challenge)
      _ <- createAndRegisterChallengeForWallet(wallet).mapStoreErrors
      token <- tokenService
        .createToken(Subject(wallet.urn.toString))
        .mapError(e => AuthenticationError.Internal("Error creating token", e))
    yield token

  def validateToken(token: Token): AuthIO[Claims] =
    tokenService.parseToken(token).mapError(e => AuthenticationError.InvalidToken(token, e))

  private def createAndRegisterChallengeForWallet(wallet: Wallet): AuthStoreIO[Resource.Of[Challenge]] =
    for
      newUUID <- Random.nextUUID
      createdAt <- Clock.currentTime(TimeUnit.MILLISECONDS)
      message = Message(config.loginMessageTemplate + newUUID.toString)
      newChallenge = Challenge(newUUID, message, createdAt)
      storedChallenge <- store.registerChallengeForWallet(wallet, newChallenge)
    yield storedChallenge

  private def validateWallet(wallet: Wallet): AuthUIO =
    web3.validateWallet(wallet).mapError {
      case Web3Error.InvalidWallet(_, cause) =>
        AuthenticationError.InvalidWallet(wallet, cause)
      case other: Web3Error =>
        AuthenticationError.Internal("Internal web3 error identifying user", other)
    }

  /*
  private def validateChallenge(wallet: Wallet, inputChallenge: Challenge): AuthUIO =
    for
      storedChallenge <- store.getChallengeForWallet(wallet).mapStoreErrors
      _ <-
        if storedChallenge.uuid != inputChallenge.uuid || storedChallenge.message != inputChallenge.message
        then ZIO.fail(AuthenticationError.BadCredentials(s"Invalid or expired challenge: $inputChallenge"))
        else ZIO.unit
    yield ()
   */

  extension [R](storeIO: AuthStoreIO[R])
    def mapStoreErrors: AuthIO[R] =
      storeIO.mapError {
        case AuthStoreError.ChallengeNotFound(wallet, cause) =>
          AuthenticationError.BadCredentials(s"Challenge not found for wallet $wallet", Some(cause))
        case AuthStoreError.ErrorStoringWallet(wallet, cause) =>
          AuthenticationError.Internal(s"Error storing wallet in store: $wallet", cause)
        case AuthStoreError.ErrorStoringChallenge(challenge, cause) =>
          AuthenticationError.Internal(s"Error storing challenge in store: $challenge", cause)
        case AuthStoreError.InternalError(message, cause) =>
          AuthenticationError.Internal(s"Internal store error: $message", cause)
      }

object AuthenticationLogic:

  val default: ZLayer[
    AuthenticationServiceConfig & AuthStore & Web3Service & TokenService,
    AuthenticationError,
    AuthenticationService
  ] =
    ZLayer(
      for
        store <- ZIO.service[AuthStore]
        web3 <- ZIO.service[Web3Service]
        tokenService <- ZIO.service[TokenService]
        config <- ZIO.service[AuthenticationServiceConfig]
      yield new AuthenticationLogic(config, store, web3, tokenService)
    )
