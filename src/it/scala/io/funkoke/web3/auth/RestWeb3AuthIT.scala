/*
 * TODO: License goes here!
 */
package io.funkoke.web3.auth

import java.util.UUID
import org.web3j.crypto.*
import zio.*
import zio.http.*
import zio.json.*
import zio.test.Assertion.*
import zio.test.*
import io.funkode.web3.auth.model.*
import io.funkode.web3.auth.input.AuthenticationService
import io.funkode.web3.auth.domain.AuthenticationLogic
import io.funkode.web3.auth.input.adapters.RestAuthenticationApi
import io.funkode.web3.auth.output.adapters.*
import io.lemonlabs.uri.Urn
import zio.http.model.Method

trait SampleRequests:

  val seed = UUID.randomUUID.toString

  val ecKeyPair1 = Keys.createEcKeyPair
  val privateKey1 = ecKeyPair1.getPrivateKey.toString(16)

  val web3Wallet1 = org.web3j.crypto.Wallet.createLight(seed, ecKeyPair1)
  val walletAddress1 = Keys.toChecksumAddress("0x" + web3Wallet1.getAddress)

  def signMessage(
      ecKeyPair: ECKeyPair,
      message: String
  ): IO[AuthenticationError, String] =
    val messageBytes = message.getBytes
    val signatureData = Sign.signPrefixedMessage(messageBytes, ecKeyPair)

    val value = new Array[Byte](65)

    Array.copy(signatureData.getR, 0, value, 0, 32)
    Array.copy(signatureData.getS, 0, value, 32, 32)
    Array.copy(signatureData.getV, 0, value, 64, 1)

    ZIO.succeed(org.web3j.utils.Numeric.toHexString(value))

object RestAuthIT extends ZIOSpecDefault with SampleRequests:

  import RestAuthenticationApi.app

  override def spec: Spec[TestEnvironment, Any] =
    suite("Rest Auth app should")(
      test(
        "Create challenge from wallet, validate signature and create login token and get wallet info from token"
      ) {
        for
          challengeResponse <- app.runZIO(Request.post(Body.empty, URL(!! / "challenge" / walletAddress1)))
          loginChallenge <- challengeResponse.body.asString
          signature <- signMessage(ecKeyPair1, loginChallenge)
          loginRequestJson = s"""{"message": "$loginChallenge", "signature": "$signature"}"""
          tokenRequest = Request.post(Body.fromString(loginRequestJson), URL(!! / "login" / walletAddress1))
          tokenResponse <- app.runZIO(tokenRequest)
          token <- tokenResponse.body.asString
          claimsRequest = Request.default(Method.GET, URL(!! / "claims"), Body.fromString(token))
          claimsResponse <- app.runZIO(claimsRequest).flatMap(_.body.asString)
          claims <- ZIO.fromEither(claimsResponse.fromJson[Claims])
        yield assertTrue(claims.sub.unwrap == s"urn:wallet:evm:$walletAddress1")
      }
    ).provideShared(
      EvmWeb3Provider.live,
      JwtConfig.default,
      JwtTokenProvider.live,
      ZioResourceAuthStore.testContainers,
      AuthenticationLogic.default
    )
