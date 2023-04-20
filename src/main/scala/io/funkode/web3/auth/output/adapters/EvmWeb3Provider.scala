/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.output
package adapters

import java.math.BigInteger

import org.web3j.crypto.{ECDSASignature, Hash, Keys}
import org.web3j.crypto.Keys.toChecksumAddress
import org.web3j.crypto.Sign.{recoverFromSignature, SignatureData}
import org.web3j.utils.Numeric.hexStringToByteArray
import zio.{ZIO, ZLayer}

import io.funkode.web3.auth.model.*

class EvmWeb3Provider extends Web3Service:

  import EvmWeb3Provider.*

  def validateWallet(wallet: Wallet): Web3UIO =

    val walletAddress = wallet.walletAddress.unwrap

    if EvmAddressRegex.matches(walletAddress.toLowerCase) && walletAddress == wallet.checksumAddress
    then ZIO.succeed(())
    else ZIO.fail(Web3Error.InvalidWallet(wallet))

  def validateSignature(wallet: Wallet, msg: Message, signature: Signature): Web3UIO =
    val prefixedMessage = PersonalMessagePrefix + msg.unwrap.length + msg.unwrap
    val messageHash = Hash.sha3(prefixedMessage.getBytes)
    val canonicalAddress = wallet.checksumAddress

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

      if candidate != null && toChecksumAddress("0x" + Keys.getAddress(candidate)) == canonicalAddress
      then found = true

      i = i + 1

    if found then ZIO.succeed(()) else ZIO.fail(Web3Error.InvalidSignature)

  extension (wallet: Wallet) def checksumAddress: String = toChecksumAddress(wallet.walletAddress.unwrap)

object EvmWeb3Provider:

  val EvmAddressRegex = "^0x[0-9a-f]{40}$".r
  val PersonalMessagePrefix = "\u0019Ethereum Signed Message:\n"

  val Index0 = 0
  val Number27 = 27
  val Index32 = 32
  val Index64 = 64

  val live: ZLayer[Any, Web3Error, Web3Service] = ZLayer(ZIO.succeed(new EvmWeb3Provider))
