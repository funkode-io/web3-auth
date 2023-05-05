/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.output.adapters

import zio.config.*
import zio.config.magnolia.*
import zio.config.typesafe.*

case class JwtConfig(signingKey: String)

object JwtConfig:

  import ConfigDescriptor.nested

  val DefaultPath = "jwt"

  val jwtConfigDescriptor = descriptor[JwtConfig].mapKey(toKebabCase)

  def fromPath(path: String) = TypesafeConfig.fromResourcePath(nested(path)(jwtConfigDescriptor))

  val default = fromPath(DefaultPath)
