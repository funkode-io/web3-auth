/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth

import java.net.{InetAddress, InetSocketAddress}

import zio.*
import zio.http.*
import zio.http.model.*
import zio.http.middleware.RequestHandlerMiddlewares
import zio.http.middleware.HttpRoutesMiddlewares.cors
import zio.http.middleware.Cors.*

object Main extends ZIOAppDefault:

  import input.*
  import input.adapters.*
  import domain.AuthenticationLogic
  import output.adapters.*

  val serverConfig =
    ZLayer:
      for restConfig <- ZIO.service[RestApiConfig]
      yield ServerConfig(
        address = new InetSocketAddress(InetAddress.getByName(restConfig.host), restConfig.port)
      )

  val corsConfig: CorsConfig =
    CorsConfig(
      allowedOrigins = _ => true,
      allowedMethods = Some(Set(Method.GET, Method.POST, Method.PUT, Method.DELETE))
    )

  val authApp = RestAuthenticationApi.app @@ RequestHandlerMiddlewares.debug @@ cors(corsConfig)

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
