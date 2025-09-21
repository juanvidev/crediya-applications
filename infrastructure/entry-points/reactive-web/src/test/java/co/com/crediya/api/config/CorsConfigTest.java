package co.com.crediya.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@ContextConfiguration(classes = {
        CorsConfig.class,
        SecurityHeadersConfig.class,
        CorsConfigTest.TestRouter.class,
        CorsConfigTest.SecurityOverride.class
})
@WebFluxTest
class CorsConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @Configuration
    static class SecurityOverride {
        @Bean
        public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
            return http
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .authorizeExchange(auth -> auth
                            .pathMatchers(
                                    "/test"
                            ).permitAll()).build();
        }
    }

    @Test
    void testGetShouldReturnOk() {
        webTestClient.get()
                .uri("/test")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Content-Security-Policy",
                        "default-src 'self'; frame-ancestors 'self'; form-action 'self'")
                .expectHeader().valueEquals("Strict-Transport-Security", "max-age=31536000;")
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                .expectHeader().valueEquals("Server", "")
                .expectHeader().valueEquals("Cache-Control", "no-store")
                .expectHeader().valueEquals("Pragma", "no-cache")
                .expectHeader().valueEquals("Referrer-Policy", "strict-origin-when-cross-origin")
                .expectBody(String.class)
                .consumeWith(body -> {
                    Mono.justOrEmpty(body.getResponseBody())
                            .defaultIfEmpty("EMPTY")
                            .map(String::toUpperCase)
                            .block();
                });
    }

    @Test
    void testPostShouldReturnNoContent() {
        webTestClient.post()
                .uri("/test")

                .exchange()
                .expectStatus().isNoContent()
                .expectHeader().valueEquals("Content-Security-Policy",
                        "default-src 'self'; frame-ancestors 'self'; form-action 'self'")
                .expectHeader().valueEquals("Strict-Transport-Security", "max-age=31536000;")
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                .expectHeader().valueEquals("Server", "")
                .expectHeader().valueEquals("Cache-Control", "no-store")
                .expectHeader().valueEquals("Pragma", "no-cache")
                .expectHeader().valueEquals("Referrer-Policy", "strict-origin-when-cross-origin")
                .expectBody(String.class)
                .consumeWith(body -> {
                    Mono.justOrEmpty(body.getResponseBody())
                            .defaultIfEmpty("EMPTY")
                            .map(String::length)
                            .block();
                });
    }

    @Configuration
    static class TestRouter {
        @Bean
        public RouterFunction<ServerResponse> testRoute() {
            return route()
                    .GET("/test", req ->
                            Mono.just("ok")
                                    .flatMap(val -> ServerResponse.ok().bodyValue(val))
                    )
                    .POST("/test", req ->
                            Mono.just(req)
                                    .flatMap(r -> ServerResponse.noContent().build())
                    )
                    .build();
        }
    }


}