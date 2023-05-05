/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.output
package adapters

import io.funkode.resource.model.*
import io.funkode.resource.output.*
import zio.*
import zio.json.*

import io.funkode.web3.auth.model.*

class ZioResourceAuthStore(store: ResourceStore) extends AuthStore:

  import ResourceStore.{body, ifNotFound}

  def getChallengeForWallet(wallet: Wallet): AuthStoreIO[Challenge] =
    for
      _ <- store
        .fetchOneAs[Wallet](wallet.urn)
        .mapError {
          case e: ResourceError.NotFoundError => AuthStoreError.ChallengeNotFound(wallet, e)
          case e => AuthStoreError.InternalError("Error retrieving wallet from store", e)
        }
      challenge <- store
        .fetchOneRelAs[Challenge](wallet.urn, Wallet.ChallengedBy)
        .body
        .mapError {
          case e: ResourceError.NotFoundError => AuthStoreError.ChallengeNotFound(wallet, e)
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
        .fetchRelAs[Challenge](wallet.urn, Wallet.ChallengedBy)
        .catchSome { case _: ResourceError.NotFoundError =>
          zio.stream.ZStream.empty
        }
        .map(_.urn)
        .runForeach(store.delete)
        .mapError(e => AuthStoreError.ErrorStoringChallenge(challenge, e)) *>
      store
        .save[Challenge](challenge)
        .mapError(e => AuthStoreError.ErrorStoringChallenge(challenge, e)) *>
      store
        .link(wallet.urn, Wallet.ChallengedBy, challenge.urn)
        .mapError(e => AuthStoreError.ErrorStoringChallenge(challenge, e))

object ZioResourceAuthStore:

  val live: ZLayer[ResourceStore, Nothing, AuthStore] =
    ZLayer(ZIO.service[ResourceStore].map(store => ZioResourceAuthStore(store)))
