/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.model

enum TokenError extends Throwable:
  case MissingSubject(token: Token)
  case WrongTokenFormat(token: Token, cause: Throwable)

  override def getCause: Throwable = this match
    case TokenError.WrongTokenFormat(_, cause) => cause
    case _                                     => null

  override def getMessage: String = this match
    // TODO encrypt token for error message
    case MissingSubject(_)                     => "Wrong token, missing subject"
    case TokenError.WrongTokenFormat(_, cause) => s"Wrong token format, ${cause.getMessage}"
