/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth

import java.net.{InetAddress, InetSocketAddress}

import io.funkode.resource.output.adapter.*
import zio.*
import zio.http.*
import zio.http.middleware.RequestHandlerMiddlewares

object Main extends ZIOAppDefault:

  import input.*
  import input.adapters.*
  import domain.AuthenticationLogic
  import output.adapters.*

  val authApp = RestAuthenticationApi.app @@ RequestHandlerMiddlewares.debug

  val serverConfig =
    ZLayer {
      for restConfig <- ZIO.service[RestApiConfig]
      yield ServerConfig(
        address = new InetSocketAddress(InetAddress.getByName(restConfig.host), restConfig.port)
      )
    }

  override val run =
    Server
      .serve[AuthenticationService](authApp)
      .provide(
        AuthenticationServiceConfig.default,
        RestApiConfig.default,
        serverConfig,
        Server.live,
        EvmWeb3Provider.live,
        JwtConfig.default,
        JwtTokenProvider.live,
        ZioResourceAuthStore.live,
        AuthenticationLogic.default
      )
