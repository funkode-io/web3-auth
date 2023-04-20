/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.model

enum TokenError extends Throwable:
  case MissingSubject
  case WrongTokenFormat(message: String, cause: Throwable)

  override def getCause: Throwable = this match
    case TokenError.WrongTokenFormat(_, cause) => cause
    case _                                     => null

  override def getMessage: String = this match
    case MissingSubject                          => "Wrong token, missing subject"
    case TokenError.WrongTokenFormat(message, _) => message
