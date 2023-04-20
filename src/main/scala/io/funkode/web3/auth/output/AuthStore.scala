/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.output

import io.funkode.resource.model.*
import io.funkode.resource.output.*
import io.funkode.resource.output.ResourceStore.{body, ifNotFound}
import zio.*
import zio.json.*

import io.funkode.web3.auth.model.*

type AuthStoreIO[R] = IO[AuthStoreError, R]
type AuthStoreUIO = AuthStoreIO[Unit]

trait AuthStore:

  def getChallengeForWallet(wallet: Wallet): AuthStoreIO[Challenge]
  def registerChallengeForWallet(wallet: Wallet, challenge: Challenge): AuthStoreUIO

object AuthStore:

  extension [R, A](storeIO: ZIO[R, AuthStoreError, A])
    def ifNotFound(f: => ZIO[R, AuthStoreError, A]): ZIO[R, AuthStoreError, A] =
      storeIO.catchSome { case _: AuthStoreError.ChallengeNotFound =>
        f
      }

class ZioResourceAuthStore(store: ResourceStore) extends AuthStore:

  def getChallengeForWallet(wallet: Wallet): AuthStoreIO[Challenge] =
    for
      _ <- store
        .fetchOneAs[Wallet](wallet.urn)
        .mapError {
          case _: ResourceError.NotFoundError => AuthStoreError.ChallengeNotFound(wallet)
          case e => AuthStoreError.InternalError("Error retrieving wallet from store", e)
        }
      challenge <- store
        .fetchOneRelAs[Challenge](wallet.urn, Wallet.ChallengedBy)
        .body
        .mapError {
          case _: ResourceError.NotFoundError => AuthStoreError.ChallengeNotFound(wallet)
          case e => AuthStoreError.InternalError("Error retrieving challenge from store", e)
        }
    yield challenge

  def registerChallengeForWallet(wallet: Wallet, challenge: Challenge): AuthStoreUIO =
    // TODO persist challenge for wallet inside a transaction
    store
      .fetchOneAs[Wallet](wallet.urn)
      .ifNotFound(_ => store.save(wallet))
      .mapError(e => AuthStoreError.ErrorStoringWallet(wallet, e)) *>
      store
        .save[Challenge](challenge)
        .mapError(e => AuthStoreError.ErrorStoringChallenge(challenge, e)) *>
      store
        .link(wallet.urn, Wallet.ChallengedBy, challenge.urn)
        .mapError(e => AuthStoreError.ErrorStoringChallenge(challenge, e))
