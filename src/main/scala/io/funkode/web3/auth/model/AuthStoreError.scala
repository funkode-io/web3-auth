/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.model

enum AuthStoreError extends Throwable:
  case ChallengeNotFound(wallet: Wallet)
  case ErrorStoringWallet(wallet: Wallet, cause: Throwable)
  case ErrorStoringChallenge(challenge: Challenge, cause: Throwable)
  case InternalError(message: String, cause: Throwable)

  override def getMessage: String = this match
    case ChallengeNotFound(wallet)           => s"Challenge not found for wallet: $wallet"
    case ErrorStoringWallet(wallet, _)       => s"Error storing wallet: $wallet"
    case ErrorStoringChallenge(challenge, _) => s"Error storing challenge: $challenge"
    case InternalError(message, _)           => s"Error with auth store: $message"

  override def getCause: Throwable = this match
    case ErrorStoringWallet(_, cause)    => cause
    case ErrorStoringChallenge(_, cause) => cause
    case InternalError(_, cause)         => cause
    case _                               => null
