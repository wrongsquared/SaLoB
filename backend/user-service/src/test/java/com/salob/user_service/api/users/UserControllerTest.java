package com.salob.user_service.api.users;

import com.salob.user_service.api.users.dto.MeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserController controller = new UserController(userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void me_returnsUserProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        MeResponse response = MeResponse.builder()
                .id(userId)
                .email("test@example.com")
                .username("testuser")
                .roles(List.of("CONTRIBUTOR"))
                .avatarUrl("https://avatar.url")
                .build();

        when(userService.me(userId)).thenReturn(response);

        mockMvc.perform(get("/api/users/me")
                        .header("X-User-Id", userId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.roles[0]").value("CONTRIBUTOR"));
    }
}
