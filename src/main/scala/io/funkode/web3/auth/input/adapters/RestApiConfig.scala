/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.input.adapters

import zio.config.*
import zio.config.magnolia.*
import zio.config.typesafe.*

case class RestApiConfig(host: String, port: Int)

object RestApiConfig:

  import ConfigDescriptor.nested

  val DefaultPath = "rest"

  val restConfigDescriptor = descriptor[RestApiConfig].mapKey(toKebabCase)

  def fromPath(path: String) = TypesafeConfig.fromResourcePath(nested(path)(restConfigDescriptor))

  val default = fromPath(DefaultPath)
