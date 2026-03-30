package com.blogplatform.service;

import com.blogplatform.dto.AuthDto;
import com.blogplatform.exception.BadRequestException;
import com.blogplatform.exception.DuplicateResourceException;
import com.blogplatform.exception.TokenException;
import com.blogplatform.model.*;
import com.blogplatform.repository.*;
import com.blogplatform.util.EmailService;
import com.blogplatform.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock UserRepository               userRepository;
    @Mock RefreshTokenRepository       refreshTokenRepository;
    @Mock EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock AuthenticationManager        authenticationManager;
    @Mock JwtUtil                      jwtUtil;
    @Mock EmailService                 emailService;

    @Spy  PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @InjectMocks AuthService authService;

    private AuthDto.RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        // Set refreshExpirationMs via reflection
        try {
            var field = AuthService.class.getDeclaredField("refreshExpirationMs");
            field.setAccessible(true);
            field.set(authService, 604800000L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        registerRequest = new AuthDto.RegisterRequest();
        registerRequest.setUsername("johndoe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("Password@1");
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
    }

    // ---- Register ----

    @Test
    @DisplayName("register: success — saves user and sends verification email")
    void register_success() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByUsername(any())).thenReturn(false);
        User saved = User.builder().id(1L).username("johndoe").email("john@example.com")
                .firstName("John").lastName("Doe").role(User.Role.ROLE_USER).enabled(false).build();
        when(userRepository.save(any())).thenReturn(saved);
        when(emailVerificationTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AuthDto.AuthResponse response = authService.register(registerRequest);

        assertThat(response.getUser().getUsername()).isEqualTo("johndoe");
        verify(emailService).sendVerificationEmail(eq("john@example.com"), eq("johndoe"), anyString());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register: throws DuplicateResourceException when email exists")
    void register_emailExists_throws() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    @DisplayName("register: throws DuplicateResourceException when username exists")
    void register_usernameExists_throws() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByUsername("johndoe")).thenReturn(true);
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username already taken");
    }

    // ---- Login ----

    @Test
    @DisplayName("login: success — returns tokens")
    void login_success() {
        User user = User.builder().id(1L).username("johndoe").email("john@example.com")
                .firstName("John").lastName("Doe").role(User.Role.ROLE_USER).enabled(true).build();

        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtil.generateToken(user)).thenReturn("access-token");
        when(jwtUtil.getExpirationMs()).thenReturn(86400000L);
        when(refreshTokenRepository.save(any())).thenAnswer(i -> {
            RefreshToken rt = i.getArgument(0);
            rt = RefreshToken.builder().token("refresh-token").user(user)
                    .expiresAt(LocalDateTime.now().plusDays(7)).build();
            return rt;
        });

        AuthDto.LoginRequest loginRequest = new AuthDto.LoginRequest();
        loginRequest.setUsernameOrEmail("johndoe");
        loginRequest.setPassword("Password@1");

        AuthDto.AuthResponse response = authService.login(loginRequest);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getUser().getUsername()).isEqualTo("johndoe");
    }

    // ---- Verify Email ----

    @Test
    @DisplayName("verifyEmail: success — enables user")
    void verifyEmail_success() {
        User user = User.builder().id(1L).username("johndoe").email("john@example.com")
                .enabled(false).build();
        EmailVerificationToken token = EmailVerificationToken.builder()
                .token("valid-token").user(user).used(false)
                .expiresAt(LocalDateTime.now().plusHours(1)).build();

        when(emailVerificationTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(emailVerificationTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        authService.verifyEmail("valid-token");

        assertThat(user.isEnabled()).isTrue();
        verify(emailService).sendWelcomeEmail(any(), any());
    }

    @Test
    @DisplayName("verifyEmail: throws TokenException for expired token")
    void verifyEmail_expired_throws() {
        User user = User.builder().id(1L).build();
        EmailVerificationToken token = EmailVerificationToken.builder()
                .token("expired-token").user(user).used(false)
                .expiresAt(LocalDateTime.now().minusHours(1)).build();

        when(emailVerificationTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.verifyEmail("expired-token"))
                .isInstanceOf(TokenException.class)
                .hasMessageContaining("expired");
    }

    // ---- Change Password ----

    @Test
    @DisplayName("changePassword: throws BadRequestException if current password wrong")
    void changePassword_wrongCurrentPassword_throws() {
        User user = User.builder().id(1L)
                .password(passwordEncoder.encode("CorrectPass@1")).build();

        AuthDto.ChangePasswordRequest req = new AuthDto.ChangePasswordRequest();
        req.setCurrentPassword("WrongPass@1");
        req.setNewPassword("NewPass@123");

        assertThatThrownBy(() -> authService.changePassword(user, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Current password is incorrect");
    }

    @Test
    @DisplayName("changePassword: success — updates password and invalidates refresh tokens")
    void changePassword_success() {
        User user = User.builder().id(1L)
                .password(passwordEncoder.encode("OldPass@1")).build();

        AuthDto.ChangePasswordRequest req = new AuthDto.ChangePasswordRequest();
        req.setCurrentPassword("OldPass@1");
        req.setNewPassword("NewPass@123");

        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        authService.changePassword(user, req);

        verify(refreshTokenRepository).deleteByUser(user);
        assertThat(passwordEncoder.matches("NewPass@123", user.getPassword())).isTrue();
    }
}
