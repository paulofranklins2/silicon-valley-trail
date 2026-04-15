package com.pcunha.svt.application;

import com.pcunha.svt.domain.model.AuthSession;
import com.pcunha.svt.domain.model.UserAccount;
import com.pcunha.svt.domain.port.AuthSessionPort;
import com.pcunha.svt.domain.port.UserPort;
import com.pcunha.svt.infrastructure.web.AuthCookies;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public class AuthService {
    private static final Pattern LOGIN_PATTERN = Pattern.compile("^[a-z0-9_\\-]{3,24}$");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserPort userPort;
    private final AuthSessionPort authSessionPort;
    private final PasswordEncoder passwordEncoder;
    private final RoomService roomService;

    public AuthService(UserPort userPort, AuthSessionPort authSessionPort,
                       PasswordEncoder passwordEncoder, RoomService roomService) {
        this.userPort = userPort;
        this.authSessionPort = authSessionPort;
        this.passwordEncoder = passwordEncoder;
        this.roomService = roomService;
    }

    public Optional<UserAccount> resolveUser(HttpServletRequest request, HttpServletResponse response) {
        String authToken = AuthCookies.read(request);
        if (authToken == null || authToken.isBlank()) return Optional.empty();

        Optional<AuthSession> session = findSessionByToken(authToken);
        if (session.isEmpty()) {
            AuthCookies.clear(request, response);
            return Optional.empty();
        }

        if (session.get().getExpiresAt().isBefore(LocalDateTime.now())) {
            authSessionPort.deleteById(session.get().getId());
            AuthCookies.clear(request, response);
            return Optional.empty();
        }

        Optional<UserAccount> user = userPort.findById(session.get().getUserId());
        if (user.isEmpty()) {
            authSessionPort.deleteById(session.get().getId());
            AuthCookies.clear(request, response);
            return Optional.empty();
        }

        return user;
    }

    public AuthResult signup(String rawLogin, String password, String guestToken,
                             HttpServletRequest request, HttpServletResponse response) {
        String login = normalizeLogin(rawLogin);
        if (!LOGIN_PATTERN.matcher(login).matches()) {
            return AuthResult.error("Invalid credentials");
        }
        if (password == null || password.length() < 8) {
            return AuthResult.error("Invalid credentials");
        }
        if (userPort.findByLogin(login).isPresent()) {
            return AuthResult.error("Invalid credentials");
        }

        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID().toString());
        user.setLogin(login);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setCreatedAt(LocalDateTime.now());
        userPort.save(user);

        createSession(user, request, response);
        roomService.attachGuestSessionToUser(guestToken, user.getId());
        return AuthResult.success(user);
    }

    public AuthResult login(String rawLogin, String password, String guestToken,
                            HttpServletRequest request, HttpServletResponse response) {
        String login = normalizeLogin(rawLogin);
        Optional<UserAccount> user = userPort.findByLogin(login);
        if (user.isEmpty() || password == null || !passwordEncoder.matches(password, user.get().getPasswordHash())) {
            return AuthResult.error("Invalid credentials");
        }

        createSession(user.get(), request, response);
        roomService.attachGuestSessionToUser(guestToken, user.get().getId());
        return AuthResult.success(user.get());
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String authToken = AuthCookies.read(request);
        if (authToken != null && !authToken.isBlank()) {
            String tokenHash = hashToken(authToken);
            authSessionPort.deleteById(tokenHash);
            authSessionPort.deleteById(authToken);
        }
        AuthCookies.clear(request, response);
    }

    private void createSession(UserAccount user, HttpServletRequest request, HttpServletResponse response) {
        String authToken = generateSessionToken();
        AuthSession session = new AuthSession();
        session.setId(hashToken(authToken));
        session.setUserId(user.getId());
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusDays(30));
        authSessionPort.save(session);
        AuthCookies.write(request, response, authToken);
    }

    private Optional<AuthSession> findSessionByToken(String authToken) {
        String tokenHash = hashToken(authToken);
        Optional<AuthSession> hashedSession = authSessionPort.findById(tokenHash);
        if (hashedSession.isPresent()) {
            return hashedSession;
        }

        return authSessionPort.findById(authToken)
                .map(session -> migrateLegacySession(authToken, session));
    }

    private AuthSession migrateLegacySession(String authToken, AuthSession session) {
        AuthSession migratedSession = new AuthSession();
        migratedSession.setId(hashToken(authToken));
        migratedSession.setUserId(session.getUserId());
        migratedSession.setCreatedAt(session.getCreatedAt());
        migratedSession.setExpiresAt(session.getExpiresAt());
        authSessionPort.deleteById(session.getId());
        return authSessionPort.save(migratedSession);
    }

    private static String generateSessionToken() {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private static String hashToken(String authToken) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(authToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String normalizeLogin(String rawLogin) {
        return rawLogin == null ? "" : rawLogin.trim().toLowerCase();
    }

    public record AuthResult(boolean ok, String error, UserAccount user) {
        static AuthResult success(UserAccount user) {
            return new AuthResult(true, null, user);
        }

        static AuthResult error(String error) {
            return new AuthResult(false, error, null);
        }
    }
}
