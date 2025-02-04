package no.nav.security.mock.oauth2.grant

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.oauth2.sdk.JWTBearerGrant
import com.nimbusds.oauth2.sdk.OAuth2Error
import com.nimbusds.oauth2.sdk.TokenRequest
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.extensions.expiresIn
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.http.OAuth2TokenResponse
import no.nav.security.mock.oauth2.invalidRequest
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import okhttp3.HttpUrl

internal class JwtBearerGrantHandler(private val tokenProvider: OAuth2TokenProvider) : GrantHandler {

    override fun tokenResponse(
        request: OAuth2HttpRequest,
        issuerUrl: HttpUrl,
        oAuth2TokenCallback: OAuth2TokenCallback,
    ): OAuth2TokenResponse {
        val tokenRequest = request.asNimbusTokenRequest()
        val receivedClaimsSet = tokenRequest.assertion()
        val accessToken = tokenProvider.exchangeAccessToken(
            tokenRequest,
            issuerUrl,
            receivedClaimsSet,
            oAuth2TokenCallback,
        )
        return OAuth2TokenResponse(
            tokenType = "Bearer",
            accessToken = accessToken.serialize(),
            expiresIn = accessToken.expiresIn(),
            scope = tokenRequest.responseScope(),
        )
    }

    private fun TokenRequest.responseScope(): String {
        return scope?.toString()
            ?: assertion().getClaim("scope")?.toString()
            ?: invalidRequest("scope must be specified in request or as a claim in assertion parameter")
    }

    private fun TokenRequest.assertion(): JWTClaimsSet =
        (this.authorizationGrant as? JWTBearerGrant)?.jwtAssertion?.jwtClaimsSet
            ?: throw OAuth2Exception(OAuth2Error.INVALID_REQUEST, "missing required parameter assertion")
}
