/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.input

import zio.config.*
import zio.config.magnolia.*
import zio.config.typesafe.*

case class AuthenticationServiceConfig(loginMessageTemplate: String)

object AuthenticationServiceConfig:

  import ConfigDescriptor.nested

  val DefaultPath = "auth"

  val authConfigDescriptor = descriptor[AuthenticationServiceConfig].mapKey(toKebabCase)

  def fromPath(path: String) = TypesafeConfig.fromResourcePath(nested(path)(authConfigDescriptor))

  val default = fromPath(DefaultPath)
