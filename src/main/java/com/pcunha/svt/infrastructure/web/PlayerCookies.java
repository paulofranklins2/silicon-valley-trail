package com.pcunha.svt.infrastructure.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Reads or issues the player identity cookie used for resume.
 */
@NoArgsConstructor
public final class PlayerCookies {
    public static final String COOKIE_NAME = "svt_player";
    private static final int MAX_AGE_SECONDS = 60 * 60 * 24 * 30;

    public static String getOrCreate(HttpServletRequest request, HttpServletResponse response) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (COOKIE_NAME.equals(cookie.getName())) return cookie.getValue();
            }
        }
        String token = UUID.randomUUID().toString();
        write(response, token);
        return token;
    }

    public static void write(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(COOKIE_NAME, token);
        cookie.setPath("/");
        cookie.setMaxAge(MAX_AGE_SECONDS);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }
}
