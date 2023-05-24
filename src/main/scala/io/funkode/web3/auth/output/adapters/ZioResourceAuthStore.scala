/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.output
package adapters

import io.funkode.arangodb.ArangoConfiguration
import io.funkode.arangodb.http.ArangoClientJson
import io.funkode.resource.model.*
import io.funkode.resource.output.*
import io.funkode.resource.output.adapter.ArangoResourceStore
import io.lemonlabs.uri.Urn
import zio.*
import zio.json.*

import io.funkode.web3.auth.model.*

class ZioResourceAuthStore(store: ResourceStore) extends AuthStore:

  import ResourceStore.{body, ifNotFound}

  def getChallengeByUrn(urn: Urn): AuthStoreIO[Challenge] = store.fetchOneAs[Challenge](urn).body.mapError {
    case e: ResourceError.NotFoundError => AuthStoreError.ChallengeNotFound(urn, e)
    case e => AuthStoreError.InternalError(s"Error retrieving challenge $urn from store", e)
  }

  def getWalletForChallenge(challenge: Challenge): AuthStoreIO[Wallet] =
    store.fetchOneRelAs[Wallet](challenge.urn, Challenge.ChallengeFor).body.mapError {
      case e: ResourceError.NotFoundError => AuthStoreError.ChallengeNotFound(challenge.urn, e)
      case e => AuthStoreError.InternalError(s"Error retrieving challenge $challenge.urn from store", e)
    }

  def getChallengeForWallet(wallet: Wallet): AuthStoreIO[Resource.Of[Challenge]] =
    for
      _ <- store
        .fetchOneAs[Wallet](wallet.urn)
        .mapError {
          case e: ResourceError.NotFoundError => AuthStoreError.ChallengeNotFound(wallet.urn, e)
          case e => AuthStoreError.InternalError("Error retrieving wallet from store", e)
        }
      challenge <- store
        .fetchOneRelAs[Challenge](wallet.urn, Wallet.ChallengedBy)
        .mapError {
          case e: ResourceError.NotFoundError => AuthStoreError.ChallengeNotFound(wallet.urn, e)
          case e => AuthStoreError.InternalError("Error retrieving challenge from store", e)
        }
    yield challenge

  def registerChallengeForWallet(wallet: Wallet, challenge: Challenge): AuthStoreIO[Resource.Of[Challenge]] =
    // TODO persist challenge for wallet inside a transaction
    (for
      _ <- store.fetchOneAs[Wallet](wallet.urn).ifNotFound(_ => store.save(wallet))
      _ <- store
        .fetchRelAs[Challenge](wallet.urn, Wallet.ChallengedBy)
        .filter(_ != null)
        .catchSome { case _: ResourceError.NotFoundError =>
          zio.stream.ZStream.empty
        }
        .map(_.urn)
        .runForeach(store.delete)
      challengeResource <- store.save[Challenge](challenge)
      _ <- store.link(wallet.urn, Wallet.ChallengedBy, challenge.urn)
      _ <- store.link(challenge.urn, Challenge.ChallengeFor, wallet.urn)
    yield challengeResource).mapError(e => AuthStoreError.ErrorStoringChallenge(challenge, e))

object ZioResourceAuthStore:

  val default: ZLayer[ResourceStore, Nothing, AuthStore] =
    ZLayer(ZIO.service[ResourceStore].map(store => ZioResourceAuthStore(store)))

  val inMemory = ResourceStore.inMemory >>> default

  val derived = ArangoResourceStore.derived[Authentication] >>> default

  val testContainers =
    (ArangoConfiguration.default ++ zio.http.Client.default) >>> ArangoClientJson.testContainers >>> derived

  val live =
    (ArangoConfiguration.default ++ zio.http.Client.default) >>> ArangoClientJson.live >>> derived
