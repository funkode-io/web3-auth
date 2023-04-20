/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth
package input

import io.lemonlabs.uri.Urn
import zio.IO

import io.funkode.web3.auth.model.*

type AuthIO[R] = IO[AuthenticationError, R]
type AuthUIO = AuthIO[Unit]

trait AuthenticationService:

  def createChallengeMessage(wallet: Wallet): AuthIO[Message]
  def login(wallet: Wallet, challengeMessage: Message, signature: Signature): AuthIO[Token]
  def validateToken(token: Token): AuthIO[Claims]
