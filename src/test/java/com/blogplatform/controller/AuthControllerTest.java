package com.blogplatform.controller;

import com.blogplatform.dto.AuthDto;
import com.blogplatform.service.AuthService;
import com.blogplatform.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@DisplayName("AuthController Integration Tests")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthService  authService;
    @MockBean JwtUtil      jwtUtil;
    @MockBean com.blogplatform.util.SecurityUtil securityUtil;
    @MockBean com.blogplatform.repository.UserRepository userRepository;

    // ---- Register ----

    @Test
    @DisplayName("POST /auth/register — valid payload returns 201")
    void register_validPayload_returns201() throws Exception {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("Password@1");
        request.setFirstName("New");
        request.setLastName("User");

        AuthDto.AuthResponse mockResponse = AuthDto.AuthResponse.builder()
                .user(com.blogplatform.dto.UserDto.Summary.builder()
                        .id(1L).username("newuser").build())
                .build();

        when(authService.register(any())).thenReturn(mockResponse);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.username").value("newuser"));
    }

    @Test
    @DisplayName("POST /auth/register — blank username returns 400")
    void register_blankUsername_returns400() throws Exception {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest();
        request.setUsername("");
        request.setEmail("new@example.com");
        request.setPassword("Password@1");
        request.setFirstName("New");
        request.setLastName("User");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /auth/register — invalid email returns 400")
    void register_invalidEmail_returns400() throws Exception {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest();
        request.setUsername("validuser");
        request.setEmail("not-an-email");
        request.setPassword("Password@1");
        request.setFirstName("First");
        request.setLastName("Last");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---- Login ----

    @Test
    @DisplayName("POST /auth/login — valid credentials returns 200 with tokens")
    void login_validCredentials_returns200() throws Exception {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setUsernameOrEmail("user@example.com");
        request.setPassword("Password@1");

        AuthDto.AuthResponse mockResponse = AuthDto.AuthResponse.builder()
                .accessToken("jwt-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .expiresIn(86400L)
                .user(com.blogplatform.dto.UserDto.Summary.builder()
                        .id(1L).username("user").build())
                .build();

        when(authService.login(any())).thenReturn(mockResponse);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("POST /auth/login — missing password returns 400")
    void login_missingPassword_returns400() throws Exception {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setUsernameOrEmail("user@example.com");
        // password intentionally omitted

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---- Forgot Password ----

    @Test
    @DisplayName("POST /auth/forgot-password — always returns 200 (prevents email enumeration)")
    void forgotPassword_alwaysReturns200() throws Exception {
        AuthDto.ForgotPasswordRequest request = new AuthDto.ForgotPasswordRequest();
        request.setEmail("anyone@example.com");

        doNothing().when(authService).forgotPassword(any());

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
