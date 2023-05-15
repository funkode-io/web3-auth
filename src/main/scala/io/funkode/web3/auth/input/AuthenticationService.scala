/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth
package input

import io.lemonlabs.uri.Urn
import zio.*

import io.funkode.web3.auth.model.*

type AuthIO[R] = IO[AuthenticationError, R]
type AuthUIO = AuthIO[Unit]

trait AuthenticationService:

  def createChallengeMessage(wallet: Wallet): AuthIO[Message]
  def login(wallet: Wallet, challengeMessage: Message, signature: Signature): AuthIO[Token]
  def validateToken(token: Token): AuthIO[Claims]

object AuthenticationService:

  type WithAuthIO[R] = ZIO[AuthenticationService, AuthenticationError, R]

  def withAuth[R](f: AuthenticationService => WithAuthIO[R]) = ZIO.service[AuthenticationService].flatMap(f)

  def createChallengeMessage(wallet: Wallet): WithAuthIO[Message] =
    withAuth(_.createChallengeMessage(wallet))

  def login(wallet: Wallet, challengeMessage: Message, signature: Signature): WithAuthIO[Token] =
    withAuth(_.login(wallet, challengeMessage, signature))

  def validateToken(token: Token): WithAuthIO[Claims] =
    withAuth(_.validateToken(token))
