package com.czagrzebski.printhelm.web.controller;

import com.czagrzebski.printhelm.api.AuthApi;
import com.czagrzebski.printhelm.model.ApiAuthResponse;
import com.czagrzebski.printhelm.model.ApiLoginRequest;
import com.czagrzebski.printhelm.model.ApiRefreshRequest;
import com.czagrzebski.printhelm.web.mapper.AuthResponseMapper;
import com.czagrzebski.printhelm.web.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Controller
@RequestMapping(value="/api")
public class AuthenticationController implements AuthApi {

    private final static Logger logger = LogManager.getLogger(AuthenticationController.class);

    private final UserService userService;
    private final AuthResponseMapper authResponseMapper;

    public AuthenticationController(final UserService userService, final AuthResponseMapper authResponseMapper) {
        this.userService = userService;
        this.authResponseMapper = authResponseMapper;
    }

    @Override
    public ResponseEntity<ApiAuthResponse> login(ApiLoginRequest apiLoginRequest) {
        try {
            logger.info("Logging in user [Username={}]", apiLoginRequest.getUsername());
            var tokens = userService.authenticateUser(apiLoginRequest.getUsername(), apiLoginRequest.getPassword());
            setRefreshTokenInCookie(tokens.getRefreshToken());
            var apiAuthResponse = authResponseMapper.authenticationDTOToApiAuthResponse(tokens);
            return new ResponseEntity<>(apiAuthResponse, HttpStatus.OK);
        } catch (Exception e) {
            logger.info("Failed to log in user [Username={}]", apiLoginRequest.getUsername());
            throw e;
        }
    }

    @Override
    public ResponseEntity<ApiAuthResponse> refresh() {
        try {
            logger.info("Refreshing token");

            // Get the refresh token from the request cookie
            String refreshToken = getRefreshTokenFromCookie();

            if(refreshToken == null) {
                logger.info("Refresh token not found in cookie");
                throw new RuntimeException("Refresh token not found in cookie");
            }

            var tokens = userService.refreshToken(refreshToken);
            setRefreshTokenInCookie(tokens.getRefreshToken());
            var apiAuthResponse = authResponseMapper.authenticationDTOToApiAuthResponse(tokens);
            return new ResponseEntity<>(apiAuthResponse, HttpStatus.OK);
        } catch (Exception e) {
            logger.info("Failed to refresh token");
            throw e;
        }
    }

    @Override
    public ResponseEntity<Void> logout() {
        try {
            logger.info("Logging out user");
            // Clear the refresh token cookie
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                HttpServletResponse servletResponse = requestAttributes.getResponse();
                if (servletResponse != null) {
                    Cookie cookie = new Cookie("refreshToken", null);
                    cookie.setHttpOnly(true);
                    cookie.setMaxAge(0); // Delete the cookie
                    servletResponse.addCookie(cookie);
                }
            }
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            logger.info("Failed to logout user");
            throw e;
        }
    }

    private String getRefreshTokenFromCookie() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            HttpServletRequest request = requestAttributes.getRequest();
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("refreshToken".equals(cookie.getName())) {
                        return cookie.getValue();
                    }
                }
            }
        }
        return null;
    }

    private void setRefreshTokenInCookie(String refreshToken) {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            HttpServletResponse servletResponse = requestAttributes.getResponse();
            if (servletResponse != null) {
                Cookie cookie = new Cookie("refreshToken", refreshToken);
                cookie.setHttpOnly(true);
                cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days in seconds
                servletResponse.addCookie(cookie);
            }
        }
    }

}
