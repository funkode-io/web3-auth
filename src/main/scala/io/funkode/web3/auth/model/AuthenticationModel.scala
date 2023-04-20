/*
 * Copyright 2023 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.web3.auth.model

import java.util.UUID

import io.funkode.resource.model.Resource
import io.lemonlabs.uri.Urn
import zio.json.{JsonCodec, JsonDecoder, JsonEncoder}

import io.funkode.web3.auth.domain.*

case class Challenge(uuid: UUID, createdAt: Long) derives JsonCodec

object Challenge:

  val Nid = "challenge"

  given Resource.Addressable[Challenge] = new Resource.Addressable[Challenge]:
    def resourceNid: String = Challenge.Nid
    def resourceNss(r: Challenge): String = r.uuid.toString
