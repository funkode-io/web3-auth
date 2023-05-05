/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.output
package adapters

import com.fasterxml.jackson.annotation.ObjectIdGenerators.UUIDGenerator
import io.funkode.resource.output.*
import pdi.jwt.*
import zio.*
import zio.test.*
import zio.test.Assertion.*

import io.funkode.web3.auth.model.*
import io.funkode.web3.auth.output.TokenService
import io.funkode.web3.auth.output.adapters.ZioResourceAuthStoreSpec.{wallet1, wallet3}

trait WalletAndChallengeExamples:

  val wallet1 = Wallet(WalletAddress("0xef678007D18427E6022059Dbc264f27507CD1ffC"))
  val challenge1 = Challenge(java.util.UUID.fromString("e58ed763-928c-4155-bee9-fdbaaadc15f3"), -1L)
  val wallet2 = Wallet(WalletAddress("0x31b26E43651e9371C88aF3D36c14CfD938BaF4Fd"))
  val wallet3 = Wallet(WalletAddress("0x44A84615dD457f729bbbf85f009F3d2e8d484D91"))

object ZioResourceAuthStoreSpec extends ZIOSpecDefault with WalletAndChallengeExamples:

  override def spec: Spec[TestEnvironment, Any] =
    suite("Auth Store should")(
      test("Get challenge from wallet") {
        for
          expectedWalletNotFound <- AuthStore.getChallengeForWallet(wallet1).flip
          _ <- AuthStore.registerChallengeForWallet(wallet1, challenge1)
          registeredChallenge <- AuthStore.getChallengeForWallet(wallet1)
        yield assert(expectedWalletNotFound)(
          isSubtype[AuthStoreError.ChallengeNotFound](hasField("wallet", _.wallet, equalTo(wallet1)))
        ) && assertTrue(registeredChallenge == challenge1)
      },
      test("Raise error if challenge is not found") {
        for challengeNotFound <- AuthStore.getChallengeForWallet(wallet2).flip
        yield assert(challengeNotFound)(
          isSubtype[AuthStoreError.ChallengeNotFound](hasField("wallet", _.wallet, equalTo(wallet2)))
        )
      },
      test("Update challenge when register new one") {
        check(Gen.uuid, Gen.long) { (randomUuid, createdAtRandom) =>
          val randomChallenge = Challenge(randomUuid, createdAtRandom)
          for
            _ <- AuthStore.registerChallengeForWallet(wallet3, randomChallenge)
            registeredChallenge <- AuthStore.getChallengeForWallet(wallet3)
          yield assertTrue(registeredChallenge == randomChallenge)
        }
      }
    ).provideShared(
      ZioResourceAuthStore.live,
      ResourceStore.inMemory
    )
