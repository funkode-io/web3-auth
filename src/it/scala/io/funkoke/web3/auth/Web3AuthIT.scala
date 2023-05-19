/*
 * TODO: License goes here!
 */
package io.funkoke.web3.auth

import java.util.UUID

import io.funkode.web3.auth.output.adapters.*
import org.web3j.crypto.*
import zio.*
import zio.test.Assertion.*
import zio.test.*

import io.funkode.web3.auth.input.AuthenticationService
import io.funkode.web3.auth.domain.AuthenticationLogic
import io.funkode.web3.auth.model.*
import io.funkode.web3.auth.output.AuthStore

trait SampleCredentials:

  val seed = UUID.randomUUID.toString

  val ecKeyPair1 = Keys.createEcKeyPair
  val privateKey1 = ecKeyPair1.getPrivateKey.toString(16)

  val web3Wallet1 = org.web3j.crypto.Wallet.createLight(seed, ecKeyPair1)
  val wallet1 =
    io.funkode.web3.auth.model.Wallet(WalletAddress(Keys.toChecksumAddress("0x" + web3Wallet1.getAddress)))

  def signMessage(
      ecKeyPair: ECKeyPair,
      message: String
  ): IO[AuthenticationError, Signature] =
    val messageBytes = message.getBytes
    val signatureData = Sign.signPrefixedMessage(messageBytes, ecKeyPair)

    val value = new Array[Byte](65)

    Array.copy(signatureData.getR, 0, value, 0, 32)
    Array.copy(signatureData.getS, 0, value, 32, 32)
    Array.copy(signatureData.getV, 0, value, 64, 1)

    ZIO.succeed(io.funkode.web3.auth.model.Signature(org.web3j.utils.Numeric.toHexString(value)))

object Web3AuthIT extends ZIOSpecDefault with SampleCredentials:
  override def spec: Spec[TestEnvironment, Any] =
    suite("Auth app should")(
      test(
        "Create challenge from wallet, validate signature and create login token and get wallet info from token"
      ) {
        for
          loginChallenge <- AuthenticationService.createChallengeMessage(wallet1)
          signature <- signMessage(ecKeyPair1, loginChallenge.unwrap)
          token <- AuthenticationService.login(wallet1, loginChallenge, signature)
          claims <- AuthenticationService.validateToken(token)
        yield assertTrue(claims.sub == io.funkode.web3.auth.model.Subject(wallet1.urn.toString))
      }
    ).provideShared(
      EvmWeb3Provider.live,
      JwtConfig.default,
      JwtTokenProvider.live,
      ZioResourceAuthStore.testContainers,
      AuthenticationLogic.default
    )
