/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.output

enum Web3Error extends Throwable:
  case InvalidWallet(walletAddress: WalletAddress, cause: Option[Throwable] = None)
  case InvalidSignature
  case Internal(msg: String, cause: Throwable)
