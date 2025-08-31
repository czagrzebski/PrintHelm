package com.czagrzebski.printhelm.web.interceptor;

import com.czagrzebski.printhelm.web.repository.UserRepository;
import com.czagrzebski.printhelm.web.service.JWTService;
import com.czagrzebski.printhelm.web.service.UserDetailsServiceImpl;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class WebsocketAuthInterceptor implements ChannelInterceptor {

    private final JWTService jwtService;
    private final UserRepository userRepository;
    private final UserDetailsServiceImpl userDetailsService;

    public WebsocketAuthInterceptor(JWTService jwtService, UserRepository userRepository, UserDetailsServiceImpl userDetailsService) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
    }

    private Message<?> Exception;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        final var accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        final var cmd = accessor.getCommand();
        String jwt = null;
        if (StompCommand.CONNECT == cmd || StompCommand.SEND == cmd) {
            final var requestTokenHeader = accessor.getFirstNativeHeader("Authorization");
            if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer")) {
                jwt = requestTokenHeader.substring(7);
            }
            var username = jwtService.extractUsername(jwt, JWTService.TokenType.ACCESS);
            var userDetails = userDetailsService.loadUserByUsername(username);

            if (!jwtService.validateToken(jwt, userDetails, JWTService.TokenType.ACCESS)) {
                return Exception;
            } else {
                var authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                accessor.setUser(authenticationToken);
            }
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            Authentication auth = (Authentication) accessor.getUser();
            String destination = accessor.getDestination();

            if (destination != null && destination.startsWith("/topic/printer/")) {
                if (auth == null || auth.getAuthorities().stream()
                        .noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                    throw new AccessDeniedException("Not allowed: " + destination);
                }
            }
        }

        return message;
    }

}