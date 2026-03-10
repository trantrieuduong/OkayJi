package com.okayji.config;

import com.okayji.chat.repository.ChatMemberRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

import static org.springframework.messaging.simp.SimpMessageType.MESSAGE;
import static org.springframework.messaging.simp.SimpMessageType.SUBSCRIBE;

@Configuration
public class WebSocketSecurityConfig {
    @Bean
    AuthorizationManager<Message<?>> messageAuthorizationManager(
            ChatMemberRepository chatMemberRepository) {
        return MessageMatcherDelegatingAuthorizationManager.builder()
                .nullDestMatcher().authenticated()

                .simpDestMatchers("/app/**").authenticated() // Client -> server (@MessageMapping)

                .simpSubscribeDestMatchers("/user/**").authenticated()

                .simpSubscribeDestMatchers("/topic/chats/{chatId}/**")
                .access((auth, ctx) -> {
                    String chatId = String.valueOf(ctx.getVariables().get("chatId"));
                    String userId = auth.get().getName();
                    boolean ok = chatMemberRepository
                            .existsByChat_IdAndMember_Id(chatId, userId);
                    return new AuthorizationDecision(ok);
                })

                .simpTypeMatchers(MESSAGE, SUBSCRIBE).denyAll()
                .anyMessage().denyAll()
                .build();
    }
}
