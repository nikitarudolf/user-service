package com.innowise.user_service.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createUser_shouldReturn201AndUseIdFromToken() throws Exception {
        Long id = nextUserId();

        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(id, USER_ROLE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest("Ivan", "Petrov", "ivan@example.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.email").value("ivan@example.com"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        assertThat(countRows("users")).isEqualTo(1);
    }

    @Test
    void createUser_shouldReturn400_whenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(nextUserId(), USER_ROLE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"surname\":\"Petrov\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void createUser_shouldReturn409_whenProfileAlreadyExists() throws Exception {
        Long id = createUser("Ivan", "Petrov", "ivan@example.com");

        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(id, USER_ROLE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest("Ivan", "Petrov", "other@example.com"))))
                .andExpect(status().isConflict());
    }

    @Test
    void createUser_shouldReturn409_whenEmailAlreadyExists() throws Exception {
        createUser("Ivan", "Petrov", "ivan@example.com");

        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(nextUserId(), USER_ROLE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest("Other", "User", "ivan@example.com"))))
                .andExpect(status().isConflict());
    }

    @Test
    void getUser_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", 999)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("User with id 999 not found"));
    }

    @Test
    void getAllUsers_shouldFilterByNameAndPaginate() throws Exception {
        createUser("Ivan", "Petrov", "ivan@example.com");
        createUser("Anna", "Sidorova", "anna@example.com");

        mockMvc.perform(get("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .param("name", "iv").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Ivan"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void updateUser_shouldReturnUpdatedUser_whenOwner() throws Exception {
        Long id = createUser("Ivan", "Petrov", "ivan@example.com");

        mockMvc.perform(put("/api/v1/users/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(id, USER_ROLE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest("Ivan", "Sidorov", "ivan@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.surname").value("Sidorov"));
    }

    @Test
    void deactivateUser_shouldEvictCache() throws Exception {
        Long id = createUser("Ivan", "Petrov", "ivan@example.com");
        mockMvc.perform(get("/api/v1/users/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(patch("/api/v1/users/{id}/deactivate", id)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void deleteUser_shouldDeleteUserWithCards_whenOwner() throws Exception {
        Long id = createUser("Ivan", "Petrov", "ivan@example.com");
        createCard(id, "4111111111111111");

        mockMvc.perform(delete("/api/v1/users/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(id, USER_ROLE)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isNotFound());
        assertThat(countRows("payment_cards")).isZero();
    }
}
