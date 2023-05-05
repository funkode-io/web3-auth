/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.model

import io.lemonlabs.uri.Urn

enum AuthenticationError extends Throwable:
  case InvalidWallet(invalid: Wallet, cause: Option[Throwable] = None)
  case BadCredentials(message: String, cause: Option[Throwable] = None)
  case InvalidToken(token: Token, cause: Throwable)
  case Internal(msg: String, cause: Throwable)

  override def getMessage: String = this match
    case InvalidWallet(wallet, cause) => s"Invalid wallet: $wallet, $cause"
    case BadCredentials(message, _)   => s"Bad credentials: $message"
    case InvalidToken(_, cause)       => s"Invalid token: ${cause.getMessage} "
    case Internal(msg, cause)         => s"Internal error: $msg, cause: $cause"

  override def getCause: Throwable = this match
    case InvalidWallet(_, Some(cause))  => cause
    case BadCredentials(_, Some(cause)) => cause
    case InvalidToken(_, cause)         => cause
    case Internal(_, cause)             => cause
    case _                              => null
