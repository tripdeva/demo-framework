package kr.co.demo.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import kr.co.demo.security.config.JwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * JWT 토큰 생성 및 검증.
 *
 * @author demo-framework
 * @since 1.1.0
 */
@Slf4j
public class JwtTokenProvider {

	private final SecretKey key;
	private final long accessTokenExpiration;
	private final long refreshTokenExpiration;

	/**
	 * 생성자.
	 *
	 * @param properties JWT 설정
	 */
	public JwtTokenProvider(JwtProperties properties) {
		this.key = Keys.hmacShaKeyFor(
				properties.secret().getBytes(StandardCharsets.UTF_8));
		this.accessTokenExpiration = properties.accessTokenExpiration();
		this.refreshTokenExpiration = properties.refreshTokenExpiration();
	}

	/**
	 * Access Token을 생성한다.
	 *
	 * @param username 사용자 ID
	 * @param roles    역할 목록
	 * @return JWT 토큰 문자열
	 */
	public String createAccessToken(String username, List<String> roles) {
		return createToken(username, roles, accessTokenExpiration);
	}

	/**
	 * Refresh Token을 생성한다.
	 *
	 * @param username 사용자 ID
	 * @return JWT 토큰 문자열
	 */
	public String createRefreshToken(String username) {
		return createToken(username, List.of(), refreshTokenExpiration);
	}

	/**
	 * 토큰에서 Authentication 객체를 추출한다.
	 *
	 * @param token JWT 토큰
	 * @return Authentication
	 */
	public Authentication getAuthentication(String token) {
		Claims claims = parseClaims(token);
		String username = claims.getSubject();

		@SuppressWarnings("unchecked")
		List<String> roles = claims.get("roles", List.class);
		List<SimpleGrantedAuthority> authorities = roles != null
				? roles.stream().map(SimpleGrantedAuthority::new).toList()
				: List.of();

		return new UsernamePasswordAuthenticationToken(username, null, authorities);
	}

	/**
	 * 토큰의 유효성을 검증한다.
	 *
	 * @param token JWT 토큰
	 * @return 유효하면 true
	 */
	public boolean validateToken(String token) {
		try {
			parseClaims(token);
			return true;
		} catch (ExpiredJwtException e) {
			log.warn("만료된 JWT 토큰입니다.");
		} catch (UnsupportedJwtException e) {
			log.warn("지원하지 않는 JWT 토큰입니다.");
		} catch (MalformedJwtException e) {
			log.warn("잘못된 JWT 토큰입니다.");
		} catch (Exception e) {
			log.warn("JWT 토큰 검증 실패: {}", e.getMessage());
		}
		return false;
	}

	/**
	 * 토큰에서 사용자 ID를 추출한다.
	 *
	 * @param token JWT 토큰
	 * @return 사용자 ID
	 */
	public String getUsername(String token) {
		return parseClaims(token).getSubject();
	}

	private String createToken(String username, List<String> roles, long expiration) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + expiration);

		return Jwts.builder()
				.subject(username)
				.claim("roles", roles)
				.issuedAt(now)
				.expiration(expiry)
				.signWith(key)
				.compact();
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}
