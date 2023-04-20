/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth
package domain

import java.util.concurrent.TimeUnit

import io.lemonlabs.uri.Urn
import org.web3j.crypto.Keys.toChecksumAddress
import pdi.jwt.Jwt
import zio.*

import io.funkode.web3.auth.input.*
import io.funkode.web3.auth.model.*
import io.funkode.web3.auth.output.*

/** Business logic for Authentication:
  *   - User will be identified from his/her wallet address
  *   - > Current version only supports evm wallets (ETH, BSC, etc.)
  *   - User needs to request a challenge before login
  *   - User has to sign the challenge with its private key
  *   - Signature is validated and a token is created with all relevant information about the user
  *   - > Token format supported: JWT
  *   - Token can be validated and then extract the information about the user (claims)
  *   - > Claims example { "sub": "urn:wallet:$wallet-address", "iac": 123456 }
  */

class AuthenticationLogic(store: AuthStore, web3: Web3Service, tokenService: TokenService)
    extends AuthenticationService:

  import AuthStore.ifNotFound

  def createChallengeMessage(wallet: Wallet): AuthIO[Message] =
    for
      _ <- validateWallet(wallet)
      challenge <- store
        .getChallengeForWallet(wallet)
        .ifNotFound(createAndRegisterChallengeForWallet(wallet))
        .mapStoreErrors
    yield challenge.toMessage

  def login(wallet: Wallet, challengeMessage: Message, signature: Signature): AuthIO[Token] =
    for
      _ <- web3
        .validateSignature(wallet, challengeMessage, signature)
        .mapError(e => AuthenticationError.BadCredentials("wrong signature", Some(e)))
      storedChallenge <- store.getChallengeForWallet(wallet).mapStoreErrors
      _ <-
        if storedChallenge.toMessage != challengeMessage
        then ZIO.fail(AuthenticationError.BadCredentials(s"Invalid or expired challenge: $challengeMessage"))
        else ZIO.unit
      token <- tokenService
        .createToken(Subject(wallet.urn.toString))
        .mapError(e => AuthenticationError.Internal("Error creating token", e))
    yield token

  def validateToken(token: Token): AuthIO[Claims] =
    tokenService.getClaims(token).mapError {
      case TokenError.MissingSubject           => AuthenticationError.InvalidToken("Missing subject")
      case TokenError.WrongTokenFormat(msg, e) => AuthenticationError.InvalidToken(msg, Some(e))
    }

  private def createAndRegisterChallengeForWallet(wallet: Wallet): AuthStoreIO[Challenge] =
    for
      newUUID <- Random.nextUUID
      createdAt <- Clock.currentTime(TimeUnit.MILLISECONDS)
      newChallenge = Challenge(newUUID, createdAt)
      _ <- store.registerChallengeForWallet(wallet, newChallenge)
    yield newChallenge

  private def validateWallet(wallet: Wallet): AuthUIO =
    web3.validateWallet(wallet).mapError {
      case Web3Error.InvalidWallet(_, cause) =>
        AuthenticationError.InvalidWallet(wallet, cause)
      case other: Web3Error =>
        AuthenticationError.Internal("Internal web3 error identifying user", other)
    }

  // TODO customized challenge message
  extension (challenge: Challenge) def toMessage: Message = Message.apply(challenge.uuid.toString)

  extension [R](storeIO: AuthStoreIO[R])
    def mapStoreErrors: AuthIO[R] =
      storeIO.mapError {
        case AuthStoreError.ChallengeNotFound(wallet) =>
          AuthenticationError.BadCredentials(s"Challenge not found for wallet $wallet")
        case AuthStoreError.ErrorStoringWallet(wallet, cause) =>
          AuthenticationError.Internal(s"Error storing wallet in store: $wallet", cause)
        case AuthStoreError.ErrorStoringChallenge(challenge, cause) =>
          AuthenticationError.Internal(s"Error storing challenge in store: $challenge", cause)
        case AuthStoreError.InternalError(message, cause) =>
          AuthenticationError.Internal(s"Internal store error: $message", cause)
      }

object AuthenticationLogic:

  val default: ZLayer[AuthStore & Web3Service & TokenService, AuthenticationError, AuthenticationService] =
    ZLayer(
      for
        store <- ZIO.service[AuthStore]
        web3 <- ZIO.service[Web3Service]
        jwt <- ZIO.service[TokenService]
      yield new AuthenticationLogic(store, web3, jwt)
    )
