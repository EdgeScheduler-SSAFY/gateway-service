package com.ssafy.gatewayservice.filter;

import com.ssafy.gatewayservice.jwt.JwtProperties;
import com.ssafy.gatewayservice.jwt.JwtTokenProvider;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
@Slf4j
public class JwtAuthenticationFilterToken extends AbstractGatewayFilterFactory<JwtAuthenticationFilterToken.Config> {

    private JwtTokenProvider jwtTokenProvider;

    @Value("${token.secret}")
    private String signingKey;

    public JwtAuthenticationFilterToken() {super(Config.class);}

    @Autowired
    public JwtAuthenticationFilterToken(JwtTokenProvider jwtTokenProvider) {
        super(Config.class);
        this.jwtTokenProvider = jwtTokenProvider;
    }
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            //Http 요청
            ServerHttpRequest request = exchange.getRequest();

            //Http header 에서 AUTHORIZATION 받아오기
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                //AUTHORIZATION 이 없으면 error return
                return onError(exchange, "No authorization header", HttpStatus.UNAUTHORIZED);
            }

            HttpHeaders headers = request.getHeaders();
            String accessToken = Objects.requireNonNull(headers.get(HttpHeaders.AUTHORIZATION)).get(0);
            String refreshToken = Objects.requireNonNull(headers.get(JwtProperties.REFRESH_TOKEN)).get(0);
            log.info("access token = {}", accessToken);
            log.info("refresh token = {}", refreshToken);

            // request 에서 토큰 파싱하기
            String token = resolveToken(request);
            // 토큰에 담겨있는 subject 저장할 변수 null 로 초기화
            String subject = null;

            // token 유효성 검사
            if (jwtTokenProvider.validateToken(token) && token != null) {


                //해당 if문 통과하면 일단 token은 유효한 것임
                log.info("JWT token validated");

                //유효한 토큰에서 subject 추출하는 작업
                JwtParser jwtParser = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build();
                subject = jwtParser.parseClaimsJws(token).getBody().getSubject();

                //추출한 subject 를 AUTHORIZATION 헤더에 담아서 필요한 microservices 들에 보내준다.
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header(HttpHeaders.AUTHORIZATION, accessToken)
                        .header(JwtProperties.REFRESH_TOKEN, refreshToken)
                        .build();
                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            }
            return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
        };
    }

    /**
     * request 에서 토큰 prefix 제거하고 access token 정보만 추출하는 메서드
     * @param request
     * @return
     */
    private String resolveToken(ServerHttpRequest request) {
        String token = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
        if (StringUtils.hasText(token) && token.startsWith(JwtProperties.TOKEN_PREFIX)) {
            return token.substring(7);
        }
        return null;
    }


    /**
     * error 처리 메서드
     * @param exchange
     * @param err
     * @param httpStatus
     * @return
     */
    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        log.error(err);

        byte[] bytes = "The requested token is invalid.".getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return response.writeWith(Flux.just(buffer));
    }

    // filter 로그에 담을 메시지 있으면 정의
    @Data
    public static class Config {
        private String baseMessage;
        private boolean preLogger;
        private boolean postLogger;
    }
}
