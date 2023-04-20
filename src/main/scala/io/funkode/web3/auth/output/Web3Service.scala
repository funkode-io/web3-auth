/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.output

import zio.*

import io.funkode.web3.auth.model.*

type Web3UIO = IO[Web3Error, Unit]

trait Web3Service:
  def validateWallet(wallet: Wallet): Web3UIO
  def validateSignature(wallet: Wallet, message: Message, signature: Signature): Web3UIO

object Web3Service:

  type WithWeb3 = ZIO[Web3Service, Web3Error, Unit]

  inline def withWeb3Service(f: Web3Service => Web3UIO): WithWeb3 =
    ZIO.service[Web3Service].flatMap(f)

  def validateWallet(wallet: Wallet): WithWeb3 =
    withWeb3Service(_.validateWallet(wallet))

  def validateSignature(wallet: Wallet, message: Message, signature: Signature): WithWeb3 =
    withWeb3Service(_.validateSignature(wallet, message, signature))
