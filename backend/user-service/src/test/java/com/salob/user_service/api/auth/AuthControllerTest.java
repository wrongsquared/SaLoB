package com.salob.user_service.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salob.user_service.api.auth.dto.LoginRequest;
import com.salob.user_service.api.auth.dto.LoginResponse;
import com.salob.user_service.api.auth.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void login_returnsOkWithJwt() throws Exception {
        LoginRequest req = new LoginRequest("test@example.com", "password");
        LoginResponse res = new LoginResponse("fake-jwt");

        when(authService.login(any(LoginRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwt").value("fake-jwt"));
    }

    @Test
    void register_returnsNoContent() throws Exception {
        RegisterRequest req = new RegisterRequest("test@example.com", "testuser", "password");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(authService).register(any(RegisterRequest.class));
    }
}
