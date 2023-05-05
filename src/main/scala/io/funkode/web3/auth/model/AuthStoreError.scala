/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.model

enum AuthStoreError extends Throwable:
  case ChallengeNotFound(wallet: Wallet, cause: Throwable)
  case ErrorStoringWallet(wallet: Wallet, cause: Throwable)
  case ErrorStoringChallenge(challenge: Challenge, cause: Throwable)
  case InternalError(message: String, cause: Throwable)

  override def getMessage: String = this match
    case ChallengeNotFound(wallet, cause) =>
      s"Challenge not found for wallet: $wallet, cause: ${cause.getMessage}"
    case ErrorStoringWallet(wallet, cause) => s"Error storing wallet: $wallet, cause: ${cause.getMessage}"
    case ErrorStoringChallenge(challenge, cause) =>
      s"Error storing challenge: $challenge, ${cause.getMessage}"
    case InternalError(message, _) => s"Error with auth store: $message"

  override def getCause: Throwable = this match
    case ErrorStoringWallet(_, cause)    => cause
    case ErrorStoringChallenge(_, cause) => cause
    case InternalError(_, cause)         => cause
    case _                               => null
