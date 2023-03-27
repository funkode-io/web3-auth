/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.output

import java.math.BigInteger

import io.funkode.resource.output.ResourceStore
import org.web3j.crypto.{ECDSASignature, Hash, Keys}
import org.web3j.crypto.Keys.toChecksumAddress
import org.web3j.crypto.Sign.{recoverFromSignature, SignatureData}
import org.web3j.utils.Numeric.hexStringToByteArray
import zio.*

type Web3IO[R] = IO[Web3Error, R]
type Web3UIO = Web3IO[Unit]

trait Web3Service:
  def validateWalletAddress(walletAddress: WalletAddress): Web3IO[WalletAddress]
  def validateSignature(wallet: WalletAddress, message: Message, signature: Signature): Web3UIO

object Web3Service:

  type WithWeb3[R] = ZIO[Web3Service, Web3Error, R]

  inline def withWeb3Service[R](f: Web3Service => Web3IO[R]): WithWeb3[R] =
    ZIO.service[Web3Service].flatMap(f)

  def validateWalletAddress(walletAddress: WalletAddress): WithWeb3[WalletAddress] =
    withWeb3Service(_.validateWalletAddress(walletAddress))

  def validateSignature(wallet: WalletAddress, message: Message, signature: Signature): WithWeb3[Unit] =
    withWeb3Service(_.validateSignature(wallet, message, signature))

  val EthAddressRegex = "^0x[0-9a-f]{40}$".r
  val PersonalMessagePrefix = "\u0019Ethereum Signed Message:\n"

  val Index0 = 0
  val Number27 = 27
  val Index32 = 32
  val Index64 = 64

  class EvmWeb3 extends Web3Service:

    def validateWalletAddress(walletAddress: WalletAddress): Web3IO[WalletAddress] =
      if EthAddressRegex.matches(walletAddress.unwrap.toLowerCase)
      then ZIO.succeed(canonical(walletAddress))
      else ZIO.fail(Web3Error.InvalidWallet(walletAddress))

    def validateSignature(username: WalletAddress, msg: Message, signature: Signature): Web3UIO =
      val prefixedMessage = PersonalMessagePrefix + msg.unwrap.length + msg.unwrap
      val messageHash = Hash.sha3(prefixedMessage.getBytes)
      val canonicalAddress = canonical(username)

      val signatureBytes = hexStringToByteArray(signature.unwrap)
      val aux = signatureBytes(Index64)

      val v: Byte = if aux < Number27.toByte then (aux + Number27.toByte).toByte else aux
      val r = java.util.Arrays.copyOfRange(signatureBytes, Index0, Index32)
      val s = java.util.Arrays.copyOfRange(signatureBytes, Index32, Index64)

      val sd = new SignatureData(v, r, s)
      val ecdaSignature = new ECDSASignature(new BigInteger(1, sd.getR), new BigInteger(1, sd.getS))

      var found = false
      var i = 0
      while !found && i < 4 do
        val candidate = recoverFromSignature(i, ecdaSignature, messageHash)

        if candidate != null && canonical(
            WalletAddress("0x" + Keys.getAddress(candidate))
          ) == canonicalAddress
        then found = true

        i = i + 1

      if found then ZIO.succeed(()) else ZIO.fail(Web3Error.InvalidSignature)

    private def canonical(walletAddress: WalletAddress): WalletAddress = WalletAddress(
      toChecksumAddress(walletAddress.unwrap)
    )

  val evm: ZLayer[Any, Web3Error, Web3Service] =
    ZLayer(ZIO.succeed(new EvmWeb3()))
