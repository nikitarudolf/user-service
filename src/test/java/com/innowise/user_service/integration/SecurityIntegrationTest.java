package com.innowise.user_service.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIntegrationTest extends AbstractIntegrationTest {

    @Test
    void request_shouldReturn401_whenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", 1))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Authentication required"));
    }

    @Test
    void request_shouldReturn401_whenTokenIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", 1)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void request_shouldReturn401_whenRefreshTokenIsUsed() throws Exception {
        Long id = createUser("Ivan", "Petrov", "ivan@example.com");
        String refreshToken = token(id, USER_ROLE, "refresh");

        mockMvc.perform(get("/api/v1/users/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUser_shouldReturn200_whenUserRequestsOwnProfile() throws Exception {
        Long id = createUser("Ivan", "Petrov", "ivan@example.com");

        mockMvc.perform(get("/api/v1/users/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(id, USER_ROLE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void getUser_shouldReturn403_whenUserRequestsOtherProfile() throws Exception {
        Long ivan = createUser("Ivan", "Petrov", "ivan@example.com");
        Long anna = createUser("Anna", "Sidorova", "anna@example.com");

        mockMvc.perform(get("/api/v1/users/{id}", anna)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ivan, USER_ROLE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Access denied"));
    }

    @Test
    void getAllUsers_shouldReturn403_whenUserIsNotAdmin() throws Exception {
        Long id = createUser("Ivan", "Petrov", "ivan@example.com");

        mockMvc.perform(get("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(id, USER_ROLE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivateUser_shouldReturn403_whenUserIsNotAdmin() throws Exception {
        Long id = createUser("Ivan", "Petrov", "ivan@example.com");

        mockMvc.perform(patch("/api/v1/users/{id}/deactivate", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(id, USER_ROLE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCard_shouldReturn403_whenUserRequestsOtherUsersCard() throws Exception {
        Long ivan = createUser("Ivan", "Petrov", "ivan@example.com");
        Long anna = createUser("Anna", "Sidorova", "anna@example.com");
        Long annaCard = createCard(anna, "5555555555554444");

        mockMvc.perform(get("/api/v1/cards/{id}", annaCard)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ivan, USER_ROLE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserCards_shouldReturn403_whenUserRequestsOtherUsersCards() throws Exception {
        Long ivan = createUser("Ivan", "Petrov", "ivan@example.com");
        Long anna = createUser("Anna", "Sidorova", "anna@example.com");

        mockMvc.perform(get("/api/v1/users/{userId}/cards", anna)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ivan, USER_ROLE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUser_shouldReturn200_whenAdminRequestsAnyProfile() throws Exception {
        Long id = createUser("Ivan", "Petrov", "ivan@example.com");

        mockMvc.perform(get("/api/v1/users/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk());
    }

    @Test
    void getCard_shouldReturn200_whenAdminRequestsAnyCard() throws Exception {
        Long id = createUser("Ivan", "Petrov", "ivan@example.com");
        Long cardId = createCard(id, "4111111111111111");

        mockMvc.perform(get("/api/v1/cards/{id}", cardId)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk());
    }
}
