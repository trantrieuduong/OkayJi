package com.okayji.config;

import com.okayji.identity.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.authorization.AuthorizationEventPublisher;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.SpringAuthorizationEventPublisher;
import org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor;
import org.springframework.security.messaging.context.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;


@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    // docs: https://docs.spring.io/spring-security/reference/servlet/integrations/websocket.html
    // access: 10/03/2026

    private final JwtService jwtService;
    private final ApplicationContext applicationContext;
    private final AuthorizationManager<Message<?>> authorizationManager;
    private final String[] ALLOW_ORIGINS;

    public WebSocketConfig(JwtService jwtService,
                           ApplicationContext applicationContext,
                           AuthorizationManager<Message<?>> authorizationManager,
                           @Value("#{'${app.front-end-domain}'.split(',')}") String[] ALLOW_ORIGINS) {
        this.jwtService = jwtService;
        this.applicationContext = applicationContext;
        this.authorizationManager = authorizationManager;
        this.ALLOW_ORIGINS = ALLOW_ORIGINS;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new AuthenticationPrincipalArgumentResolver());
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        AuthorizationChannelInterceptor authz = new AuthorizationChannelInterceptor(authorizationManager);
        AuthorizationEventPublisher publisher = new SpringAuthorizationEventPublisher(applicationContext);
        authz.setAuthorizationEventPublisher(publisher);
        registration.interceptors(new SecurityContextChannelInterceptor(), authz);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(ALLOW_ORIGINS)
                .setHandshakeHandler(new WebSocketHandshakeHandler(jwtService))
                .withSockJS();
    }
}
