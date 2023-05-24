/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.output

import io.funkode.resource.model.Resource
import io.funkode.resource.output.*
import io.lemonlabs.uri.Urn
import zio.*
import zio.json.*

import io.funkode.web3.auth.model.*

type AuthStoreIO[R] = IO[AuthStoreError, R]
type AuthStoreUIO = AuthStoreIO[Unit]

trait AuthStore:

  def getChallengeByUrn(urn: Urn): AuthStoreIO[Challenge]
  def getWalletForChallenge(challenge: Challenge): AuthStoreIO[Wallet]
  def getChallengeForWallet(wallet: Wallet): AuthStoreIO[Resource.Of[Challenge]]
  def registerChallengeForWallet(wallet: Wallet, challenge: Challenge): AuthStoreIO[Resource.Of[Challenge]]

object AuthStore:

  type WithAuthStore[R] = ZIO[AuthStore, AuthStoreError, R]

  def withAuthStore[R](f: AuthStore => WithAuthStore[R]): WithAuthStore[R] =
    ZIO.service[AuthStore].flatMap(f)

  def getChallengeForWallet(wallet: Wallet): WithAuthStore[Resource.Of[Challenge]] = withAuthStore(
    _.getChallengeForWallet(wallet)
  )

  def registerChallengeForWallet(
      wallet: Wallet,
      challenge: Challenge
  ): WithAuthStore[Resource.Of[Challenge]] =
    withAuthStore(_.registerChallengeForWallet(wallet, challenge))

  extension [R, A](storeIO: ZIO[R, AuthStoreError, A])
    def ifNotFound(f: => ZIO[R, AuthStoreError, A]): ZIO[R, AuthStoreError, A] =
      storeIO.catchSome { case _: AuthStoreError.ChallengeNotFound =>
        f
      }
