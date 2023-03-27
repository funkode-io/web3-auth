/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.output

import io.funkode.arangodb.http.DeriveOpaqueTypeCodec
import zio.json.JsonCodec

opaque type WalletAddress = String
object WalletAddress:
  def apply(value: String): WalletAddress = value
  extension (x: WalletAddress) def unwrap: String = x

opaque type Message = String
object Message:
  def apply(value: String): Message = value
  extension (x: Message) def unwrap: String = x

opaque type Signature = String
object Signature:
  def apply(value: String): Signature = value
  extension (x: Signature) def unwrap: String = x
