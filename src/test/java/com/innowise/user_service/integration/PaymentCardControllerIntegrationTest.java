package com.innowise.user_service.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentCardControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createCard_shouldReturn201AndEvictUserCache() throws Exception {
        Long userId = createUser("Ivan", "Petrov", "ivan@example.com");
        mockMvc.perform(get("/api/v1/users/{id}", userId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId, USER_ROLE)))
                .andExpect(jsonPath("$.cards.length()").value(0));

        mockMvc.perform(post("/api/v1/users/{userId}/cards", userId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId, USER_ROLE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardRequest("4111111111111111"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.userId").value(userId));

        mockMvc.perform(get("/api/v1/users/{id}", userId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId, USER_ROLE)))
                .andExpect(jsonPath("$.cards.length()").value(1));
    }

    @Test
    void createCard_shouldReturn409_whenUserAlreadyHasFiveCards() throws Exception {
        Long userId = createUser("Ivan", "Petrov", "ivan@example.com");
        for (int i = 1; i <= 5; i++) {
            createCard(userId, "400000000000000" + i);
        }

        mockMvc.perform(post("/api/v1/users/{userId}/cards", userId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId, USER_ROLE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardRequest("4000000000000006"))))
                .andExpect(status().isConflict());
    }

    @Test
    void createCard_shouldReturn409_whenNumberAlreadyExists() throws Exception {
        Long userId = createUser("Ivan", "Petrov", "ivan@example.com");
        createCard(userId, "4111111111111111");

        mockMvc.perform(post("/api/v1/users/{userId}/cards", userId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId, USER_ROLE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardRequest("4111111111111111"))))
                .andExpect(status().isConflict());
    }

    @Test
    void createCard_shouldReturn404_whenUserNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/users/{userId}/cards", 999)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardRequest("4111111111111111"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCard_shouldReturn400_whenNumberIsInvalid() throws Exception {
        Long userId = createUser("Ivan", "Petrov", "ivan@example.com");

        mockMvc.perform(post("/api/v1/users/{userId}/cards", userId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId, USER_ROLE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardRequest("abc"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.number").exists());
    }

    @Test
    void getAllCards_shouldFilterByOwnerSurname() throws Exception {
        Long ivan = createUser("Ivan", "Petrov", "ivan@example.com");
        Long anna = createUser("Anna", "Sidorova", "anna@example.com");
        createCard(ivan, "4111111111111111");
        createCard(anna, "5555555555554444");

        mockMvc.perform(get("/api/v1/cards")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .param("surname", "petrov"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(ivan));
    }

    @Test
    void updateCard_shouldReturnUpdatedCard_whenOwner() throws Exception {
        Long userId = createUser("Ivan", "Petrov", "ivan@example.com");
        Long cardId = createCard(userId, "4111111111111111");

        mockMvc.perform(put("/api/v1/cards/{id}", cardId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId, USER_ROLE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"4111111111111111\",\"holder\":\"I PETROV\",\"expirationDate\":\"2031-01-31\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holder").value("I PETROV"))
                .andExpect(jsonPath("$.expirationDate").value("2031-01-31"));
    }

    @Test
    void deactivateCard_shouldChangeStatus_whenOwner() throws Exception {
        Long userId = createUser("Ivan", "Petrov", "ivan@example.com");
        Long cardId = createCard(userId, "4111111111111111");

        mockMvc.perform(patch("/api/v1/cards/{id}/deactivate", cardId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId, USER_ROLE)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/cards/{id}", cardId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId, USER_ROLE)))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void deleteCard_shouldReturn204AndRemoveCard_whenOwner() throws Exception {
        Long userId = createUser("Ivan", "Petrov", "ivan@example.com");
        Long cardId = createCard(userId, "4111111111111111");

        mockMvc.perform(delete("/api/v1/cards/{id}", cardId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId, USER_ROLE)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/cards/{id}", cardId)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isNotFound());
    }
}
