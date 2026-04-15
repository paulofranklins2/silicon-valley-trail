package com.pcunha.svt.infrastructure.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class AuthCookies {
    public static final String COOKIE_NAME = "svt_auth";
    private static final int MAX_AGE_SECONDS = 60 * 60 * 24 * 30;

    public static String read(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    public static void write(HttpServletRequest request, HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(COOKIE_NAME, token);
        cookie.setPath("/");
        cookie.setMaxAge(MAX_AGE_SECONDS);
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecureRequest(request));
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    public static void clear(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecureRequest(request));
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private static boolean isSecureRequest(HttpServletRequest request) {
        if (request == null) return false;
        if (request.isSecure()) return true;
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto != null && forwardedProto.equalsIgnoreCase("https")) {
            return true;
        }
        String forwarded = request.getHeader("Forwarded");
        return forwarded != null && forwarded.toLowerCase().contains("proto=https");
    }
}
