package com.buy01.users.Controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
// import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.buy01.users.DTOs.LoginReqDTOs;
import com.buy01.users.DTOs.LoginResDTOs;
import com.buy01.users.DTOs.RegisterReqDTOs;
import com.buy01.users.DTOs.RegisterResDTOs;
import com.buy01.users.DTOs.ProfileResDTOs;
import com.buy01.users.DTOs.ProfileUpdateReqDTOs;
import com.buy01.users.DTOs.PublicProfileResDTO;
import com.buy01.users.Config.UserDetailServices;
import com.buy01.users.Service.AuthService;
import com.buy01.users.Service.ProfileService;
import com.buy01.users.Utils.JwtUtils;
import com.buy01.users.Exceptions.UserExistException;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest({ AuthController.class, ProfileController.class })
@SuppressWarnings("null")
class AuthAndProfileControllerTest {
        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private AuthService authService;

        @MockitoBean
        private ProfileService profileService;

        @MockitoBean
        private JwtUtils jwtUtils;

        @MockitoBean
        private UserDetailServices userDetailServices;

        @Autowired
        private ObjectMapper objectMapper;

        private RegisterReqDTOs registerReqDto;
        private RegisterResDTOs registerResDto;
        private LoginReqDTOs loginReqDto;
        private LoginResDTOs loginResDto;
        private ProfileResDTOs profileResDto;
        private ProfileUpdateReqDTOs profileUpdateReqDto;

        @BeforeEach
        void setUp() {
                registerReqDto = new RegisterReqDTOs(
                                "test@example.com",
                                "Test User",
                                "password123",
                                "BUYER",
                                null);

                registerResDto = new RegisterResDTOs("user created");

                loginReqDto = new LoginReqDTOs("test@example.com", "password123");
                loginResDto = new LoginResDTOs("jwt-token-here", "BUYER", "ok");

                profileResDto = new ProfileResDTOs(
                                "user-123",
                                "Test User",
                                "test@example.com",
                                "BUYER",
                                (UUID) null);

                profileUpdateReqDto = new ProfileUpdateReqDTOs("Updated User", null, (UUID) null);
        }

        // ===== AuthController Tests =====

        @Test
        void testRegisterSuccess() throws Exception {
                when(authService.register(any(RegisterReqDTOs.class)))
                                .thenReturn(registerResDto);

                mockMvc.perform(post("/api/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerReqDto)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.msg").value("user created"));
        }

        @Test
        void testRegisterWithAvatar() throws Exception {
                UUID avatarId = UUID.randomUUID();
                RegisterReqDTOs dtoWithAvatar = new RegisterReqDTOs(
                                "avatar@example.com",
                                "Avatar User",
                                "password123",
                                "SELLER",
                                avatarId);

                when(authService.register(any(RegisterReqDTOs.class)))
                                .thenReturn(registerResDto);

                mockMvc.perform(post("/api/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dtoWithAvatar)))
                                .andExpect(status().isOk());
        }

        @Test
        void testRegisterInvalidEmail() throws Exception {
                RegisterReqDTOs invalidDto = new RegisterReqDTOs(
                                "invalid-email",
                                "Test User",
                                "password123",
                                "BUYER",
                                null);

                mockMvc.perform(post("/api/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidDto)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testRegisterInvalidPassword() throws Exception {
                RegisterReqDTOs invalidDto = new RegisterReqDTOs(
                                "test@example.com",
                                "Test User",
                                "short",
                                "BUYER",
                                null);

                mockMvc.perform(post("/api/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidDto)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testRegisterUserAlreadyExists() throws Exception {
                when(authService.register(any(RegisterReqDTOs.class)))
                                .thenThrow(new UserExistException("Invalid Email"));

                mockMvc.perform(post("/api/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerReqDto)))
                                .andExpect(status().isConflict());
        }

        @Test
        void testLoginSuccess() throws Exception {
                when(authService.login(any(LoginReqDTOs.class)))
                                .thenReturn(loginResDto);

                mockMvc.perform(post("/api/users/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginReqDto)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").value("jwt-token-here"))
                                .andExpect(jsonPath("$.role").value("BUYER"))
                                .andExpect(jsonPath("$.message").value("ok"));
        }

        @Test
        void testLoginInvalidCredentials() throws Exception {
                LoginReqDTOs invalidLogin = new LoginReqDTOs("wrong@example.com", "wrongpassword");

                when(authService.login(any(LoginReqDTOs.class)))
                                .thenThrow(
                                                new org.springframework.security.core.userdetails.UsernameNotFoundException(
                                                                "User not found"));

                mockMvc.perform(post("/api/users/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidLogin)))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void testLoginViaAuthPath() throws Exception {
                when(authService.login(any(LoginReqDTOs.class)))
                                .thenReturn(loginResDto);

                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginReqDto)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").value("jwt-token-here"));
        }

        // ===== ProfileController Tests =====

        @Test
        void testGetPublicProfileWithoutAuthentication() throws Exception {
                when(profileService.getPublicProfile("seller-123"))
                                .thenReturn(new PublicProfileResDTO("seller-123", "Public Seller", "SELLER", (UUID) null));

                mockMvc.perform(get("/api/users/public/seller-123"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value("seller-123"))
                                .andExpect(jsonPath("$.username").value("Public Seller"))
                                .andExpect(jsonPath("$.email").doesNotExist());
        }

        @Test
        @WithMockUser(username = "user-123")
        void testGetCurrentProfileSuccess() throws Exception {
                when(profileService.getCurrentProfile())
                                .thenReturn(profileResDto);

                mockMvc.perform(get("/api/users/me")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("Test User"))
                                .andExpect(jsonPath("$.email").value("test@example.com"))
                                .andExpect(jsonPath("$.role").value("BUYER"));
        }

        @Test
        @WithMockUser(username = "user-123")
        void testUpdateCurrentProfileSuccess() throws Exception {
                ProfileResDTOs updatedProfile = new ProfileResDTOs(
                                "user-123",
                                "Updated User",
                                "test@example.com",
                                "BUYER",
                                (UUID) null);

                when(profileService.updateCurrentProfile(any(ProfileUpdateReqDTOs.class)))
                                .thenReturn(updatedProfile);

                mockMvc.perform(put("/api/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(profileUpdateReqDto)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("Updated User"));
        }

        @Test
        @WithMockUser(username = "user-123")
        void testUpdateProfileWithAvatar() throws Exception {
                UUID avatarId = UUID.randomUUID();
                ProfileUpdateReqDTOs updateWithAvatar = new ProfileUpdateReqDTOs("User With Avatar", null, avatarId);

                ProfileResDTOs updatedProfile = new ProfileResDTOs(
                                "user-123",
                                "User With Avatar",
                                "test@example.com",
                                "BUYER",
                                avatarId);

                when(profileService.updateCurrentProfile(any(ProfileUpdateReqDTOs.class)))
                                .thenReturn(updatedProfile);

                mockMvc.perform(put("/api/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateWithAvatar)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("User With Avatar"));
        }

        @Test
        @WithMockUser(username = "user-123")
        void testDeleteCurrentUserSuccess() throws Exception {
                RegisterResDTOs deleteResponse = new RegisterResDTOs("user deleted");

                when(profileService.deleteCurrentUser())
                                .thenReturn(deleteResponse);

                mockMvc.perform(delete("/api/users/me")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.msg").value("user deleted"));
        }
}
