package com.bandage.v1.global.security.jwt

import com.bandage.v1.domain.auth.model.enums.MemberRole
import com.bandage.v1.global.properties.JwtProperties
import com.bandage.v1.global.security.PrincipalDetails
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtProvider(
    private val jwtProperties: JwtProperties,
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret))
    private val accessExpr = jwtProperties.accessTokenExpr * 1000
    private val refreshExpr = jwtProperties.refreshTokenExpr * 1000

    fun createAccessToken(
        memberId: Long,
        role: MemberRole,
    ): String {
        val now = Date()
        return Jwts
            .builder()
            .subject(memberId.toString())
            .claim("role", role.value)
            .issuedAt(now)
            .expiration(Date(now.time + accessExpr))
            .signWith(key)
            .compact()
    }

    fun createRefreshToken(memberId: Long): String {
        val now = Date()
        return Jwts
            .builder()
            .subject(memberId.toString())
            .issuedAt(now)
            .expiration(Date(now.time + refreshExpr))
            .signWith(key)
            .compact()
    }

    fun validateToken(token: String): Boolean =
        try {
            Jwts
                .parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: JwtException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }

    fun getAuthentication(token: String): Authentication {
        val claims =
            Jwts
                .parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
        val memberId = claims.subject
        val role = claims["role"] as String

        val authorities = listOf(SimpleGrantedAuthority(role))
        val principal = PrincipalDetails(memberId, role)

        return UsernamePasswordAuthenticationToken(principal, token, authorities)
    }

    fun getMemberIdFromToken(token: String): Long {
        val claims =
            Jwts
                .parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
        return claims.subject.toLong()
    }
}
