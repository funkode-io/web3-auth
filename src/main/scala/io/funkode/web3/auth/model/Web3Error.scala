/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.model

enum Web3Error extends Throwable:
  case InvalidWallet(wallet: Wallet, cause: Option[Throwable] = None)
  case InvalidSignature
  case Internal(msg: String, cause: Throwable)
