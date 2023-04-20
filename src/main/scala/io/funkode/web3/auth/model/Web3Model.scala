/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.model

import io.funkode.resource.model.Resource
import zio.json.{JsonCodec, JsonDecoder, JsonEncoder}

opaque type WalletAddress = String
object WalletAddress:
  def apply(value: String): WalletAddress = value
  extension (x: WalletAddress) def unwrap: String = x

  given JsonCodec[WalletAddress] =
    JsonCodec(JsonEncoder[String].contramap(unwrap), JsonDecoder[String].map(WalletAddress.apply))

enum WalletType derives JsonCodec:
  case Evm

  override def toString: String = this match
    case Evm => "evm"

case class Wallet(walletAddress: WalletAddress, walletType: WalletType = WalletType.Evm) derives JsonCodec

object Wallet:

  val Nid = "wallet"
  val ChallengedBy = "challengedBy"

  given Resource.Addressable[Wallet] = new Resource.Addressable[Wallet]:
    def resourceNid: String = Wallet.Nid
    def resourceNss(wallet: Wallet): String = wallet.walletType.toString + ":" + wallet.walletAddress.unwrap

opaque type Message = String
object Message:
  def apply(value: String): Message = value
  extension (x: Message) def unwrap: String = x

opaque type Signature = String
object Signature:
  def apply(value: String): Signature = value
  extension (x: Signature) def unwrap: String = x
