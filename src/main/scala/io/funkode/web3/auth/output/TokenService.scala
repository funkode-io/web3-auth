/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth
package output

import zio.*

import io.funkode.web3.auth.model.*

type TokenIO[R] = IO[TokenError, R]

trait TokenService:

  def createToken(subject: Subject): TokenIO[Token]
  def getClaims(token: Token): TokenIO[Claims]

object TokenService:

  type WithTokenService[R] = ZIO[TokenService, TokenError, R]

  def withTokenService[R](f: TokenService => WithTokenService[R]): WithTokenService[R] =
    ZIO.service[TokenService].flatMap(f)

  def createToken(subject: Subject): WithTokenService[Token] = withTokenService(_.createToken(subject))

  def getClaims(token: Token): WithTokenService[Claims] = withTokenService(_.getClaims(token))
