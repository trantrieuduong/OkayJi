package com.okayji.config;

import com.okayji.identity.service.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class WebSocketHandshakeHandler extends DefaultHandshakeHandler {
    private final JwtService jwtService;

    @Override
    protected @Nullable Principal determineUser(ServerHttpRequest request,
                                                WebSocketHandler wsHandler,
                                                Map<String, Object> attributes) {
        URI uri = request.getURI();
        String token = extractQueryParam(uri, "access_token");
        Claims claims = jwtService.getClaimsJws(token).getBody();
        String userId = claims.getSubject();
        return UsernamePasswordAuthenticationToken.authenticated(
                userId,
                null,
                List.of()
        );
    }

    static String extractQueryParam(URI uri, String key) {
        String value = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing query param: " + key);
        }
        return value;
    }
}
