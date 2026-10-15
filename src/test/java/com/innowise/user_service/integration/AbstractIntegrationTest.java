package com.innowise.user_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.user_service.TestcontainersConfiguration;
import com.innowise.user_service.config.CacheConfig;
import com.innowise.user_service.dto.PaymentCardRequest;
import com.innowise.user_service.dto.PaymentCardResponse;
import com.innowise.user_service.dto.UserRequest;
import com.innowise.user_service.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.execute("TRUNCATE TABLE payment_cards, users RESTART IDENTITY CASCADE");
        cacheManager.getCache(CacheConfig.USERS_CACHE).clear();
    }

    protected UserRequest userRequest(String name, String surname, String email) {
        return new UserRequest(name, surname, LocalDate.of(2000, 1, 1), email);
    }

    protected PaymentCardRequest cardRequest(String number) {
        return new PaymentCardRequest(number, "IVAN PETROV", LocalDate.of(2030, 12, 31));
    }

    protected Long createUser(String name, String surname, String email) throws Exception {
        String json = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest(name, surname, email))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, UserResponse.class).id();
    }

    protected Long createCard(Long userId, String number) throws Exception {
        String json = mockMvc.perform(post("/api/v1/users/{userId}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardRequest(number))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, PaymentCardResponse.class).id();
    }

    protected long countRows(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }
}
