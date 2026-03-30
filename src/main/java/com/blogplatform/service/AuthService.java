package com.blogplatform.service;

import com.blogplatform.dto.AuthDto;
import com.blogplatform.dto.UserDto;
import com.blogplatform.exception.*;
import com.blogplatform.model.*;
import com.blogplatform.repository.*;
import com.blogplatform.util.EmailService;
import com.blogplatform.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already taken: " + request.getUsername());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(User.Role.ROLE_USER)
                .enabled(false)   // requires email verification
                .locked(false)
                .build();

        user = userRepository.save(user);
        sendVerificationEmail(user);

        log.info("New user registered: {}", user.getEmail());

        // Return minimal response; user must verify email before logging in
        return AuthDto.AuthResponse.builder()
                .user(mapToSummary(user))
                .build();
    }

    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail(),
                        request.getPassword()));

        User user = (User) authentication.getPrincipal();
        userRepository.updateLastLogin(user.getId(), LocalDateTime.now());

        String accessToken  = jwtUtil.generateToken(user);
        String refreshToken = createRefreshToken(user);

        log.info("User logged in: {}", user.getEmail());

        return AuthDto.AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpirationMs() / 1000)
                .user(mapToSummary(user))
                .build();
    }

    public AuthDto.AuthResponse refreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenException("Invalid refresh token"));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new TokenException("Refresh token expired. Please log in again.");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtUtil.generateToken(user);

        return AuthDto.AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpirationMs() / 1000)
                .user(mapToSummary(user))
                .build();
    }

    public void logout(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(refreshTokenRepository::delete);
    }

    public void verifyEmail(String token) {
        EmailVerificationToken evt = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenException("Invalid verification token"));

        if (evt.isUsed())    throw new TokenException("Verification token already used");
        if (evt.isExpired()) throw new TokenException("Verification token expired");

        User user = evt.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        evt.setUsed(true);
        emailVerificationTokenRepository.save(evt);

        emailService.sendWelcomeEmail(user.getEmail(), user.getUsername());
        log.info("Email verified for user: {}", user.getEmail());
    }

    public void forgotPassword(String email) {
        // Silently succeed even if email not found (prevents enumeration)
        userRepository.findByEmail(email).ifPresent(user -> {
            // Invalidate existing tokens
            passwordResetTokenRepository.deleteByUser(user);

            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .used(false)
                    .build();
            passwordResetTokenRepository.save(resetToken);
            emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), token);
            log.info("Password reset requested for: {}", email);
        });
    }

    public void resetPassword(AuthDto.ResetPasswordRequest request) {
        PasswordResetToken prt = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new TokenException("Invalid password reset token"));

        if (prt.isUsed())    throw new TokenException("Reset token already used");
        if (prt.isExpired()) throw new TokenException("Reset token has expired");

        User user = prt.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        prt.setUsed(true);
        passwordResetTokenRepository.save(prt);

        // Invalidate all refresh tokens
        refreshTokenRepository.deleteByUser(user);
        log.info("Password reset for user: {}", user.getEmail());
    }

    public void changePassword(User currentUser, AuthDto.ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);
        refreshTokenRepository.deleteByUser(currentUser);
        log.info("Password changed for user: {}", currentUser.getEmail());
    }

    // ---- Private helpers ----

    private String createRefreshToken(User user) {
        refreshTokenRepository.deleteByUser(user);
        RefreshToken rt = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000L))
                .build();
        return refreshTokenRepository.save(rt).getToken();
    }

    private void sendVerificationEmail(User user) {
        String token = UUID.randomUUID().toString();
        EmailVerificationToken evt = EmailVerificationToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();
        emailVerificationTokenRepository.save(evt);
        emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), token);
    }

    private UserDto.Summary mapToSummary(User user) {
        return UserDto.Summary.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .build();
    }
}
