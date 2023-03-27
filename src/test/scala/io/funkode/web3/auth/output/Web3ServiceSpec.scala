/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.output

import zio.Scope
import zio.test.*

trait CryptoExamples:

  val userAddress1 = WalletAddress("0xef678007d18427e6022059dbc264f27507cd1ffc")
  val canonicalAddress1 = WalletAddress("0xef678007D18427E6022059Dbc264f27507CD1ffC")
  val userAddress2 = WalletAddress("0x31b26E43651e9371C88aF3D36c14CfD938BaF4Fd")
  val userAddress3 = WalletAddress("0x44A84615dD457f729bbbf85f009F3d2e8d484D91")

  val testNonce = Message("v0G9u7huK4mJb2K1")
  val signature = Signature(
    "0x2c6401216c9031b9a6fb8cbfccab4fcec6c951cdf40e2320108d1856eb532250576865fbcd452bcdc4c57" +
      "321b619ed7a9cfd38bd973c3e1e0243ac2777fe9d5b01"
  )

  val temperedSignature = Signature(
    "0x2c6401216c9031b9a6fb8cbfccab4fc3c6c951cdf40e2320108d1856eb532250576865fbcd452bcdc4c57" +
      "321b619ed7a9cfd38bd973c3e1e0243ac2777fe9d5b02"
  )

  val wrongAddress = WalletAddress("ef678007d18427e6022059dbc264f27507cd1ffa")

object Web3ServiceSpec extends ZIOSpecDefault with CryptoExamples:
  override def spec: Spec[TestEnvironment, Any] =
    suite("Web3 service should")(
      test("Validate wallet") {
        for
          validatedAddress1 <- Web3Service.validateWalletAddress(userAddress1)
          validateWrongAddressResult <- Web3Service.validateWalletAddress(wrongAddress).flip
        yield assertTrue(validatedAddress1 == canonicalAddress1) && assertTrue(
          validateWrongAddressResult == Web3Error.InvalidWallet(wrongAddress)
        )
      },
      test("Validate correct signature") {
        for
          _ <- Web3Service.validateSignature(userAddress2, testNonce, signature)
          wrongWallet <- Web3Service.validateSignature(userAddress3, testNonce, signature).flip
          wrongSignature <- Web3Service.validateSignature(userAddress2, testNonce, temperedSignature).flip
        yield assertTrue(wrongWallet == Web3Error.InvalidSignature) && assertTrue(
          wrongSignature == Web3Error.InvalidSignature
        )
      }
    ).provideShared(
      Web3Service.evm
    )
