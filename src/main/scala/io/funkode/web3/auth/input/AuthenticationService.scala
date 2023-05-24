/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth
package input

import java.util.UUID

import io.funkode.resource.model.Resource
import io.lemonlabs.uri.Urn
import zio.*

import io.funkode.web3.auth.model.*

type AuthIO[R] = IO[AuthenticationError, R]
type AuthUIO = AuthIO[Unit]

trait AuthenticationService:

  def createLoginChallenge(wallet: Wallet): AuthIO[Resource.Of[Challenge]]
  def login(challengeUrn: Urn, signature: Signature): AuthIO[Token]
  def validateToken(token: Token): AuthIO[Claims]

object AuthenticationService:

  type WithAuthIO[R] = ZIO[AuthenticationService, AuthenticationError, R]

  def withAuth[R](f: AuthenticationService => WithAuthIO[R]) = ZIO.service[AuthenticationService].flatMap(f)

  def createLoginChallenge(wallet: Wallet): WithAuthIO[Resource.Of[Challenge]] = withAuth(
    _.createLoginChallenge(wallet)
  )

  def login(challengeUrn: Urn, signature: Signature): WithAuthIO[Token] =
    withAuth(_.login(challengeUrn, signature))

  def validateToken(token: Token): WithAuthIO[Claims] = withAuth(_.validateToken(token))
