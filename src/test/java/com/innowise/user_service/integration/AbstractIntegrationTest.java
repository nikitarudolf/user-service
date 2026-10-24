package com.innowise.user_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.user_service.TestJwtConfiguration;
import com.innowise.user_service.TestcontainersConfiguration;
import com.innowise.user_service.config.CacheConfig;
import com.innowise.user_service.dto.PaymentCardRequest;
import com.innowise.user_service.dto.PaymentCardResponse;
import com.innowise.user_service.dto.UserRequest;
import com.innowise.user_service.dto.UserResponse;
import com.innowise.user_service.security.JwtProperties;
import com.innowise.user_service.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestJwtConfiguration.class})
public abstract class AbstractIntegrationTest {

    protected static final Long ADMIN_ID = 1000L;
    protected static final String ADMIN_ROLE = "ADMIN";
    protected static final String USER_ROLE = "USER";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private JwtProperties jwtProperties;

    private long nextUserId;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.execute("TRUNCATE TABLE payment_cards, users RESTART IDENTITY CASCADE");
        cacheManager.getCache(CacheConfig.USERS_CACHE).clear();
        nextUserId = 1;
    }

    protected String token(Long userId, String role, String type) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(JwtService.ROLE_CLAIM, role)
                .claim(JwtService.TYPE_CLAIM, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(15))))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret())))
                .compact();
    }

    protected String bearer(Long userId, String role) {
        return "Bearer " + token(userId, role, JwtService.ACCESS_TOKEN_TYPE);
    }

    protected String adminBearer() {
        return bearer(ADMIN_ID, ADMIN_ROLE);
    }

    protected Long nextUserId() {
        return nextUserId++;
    }

    protected UserRequest userRequest(String name, String surname, String email) {
        return new UserRequest(name, surname, LocalDate.of(2000, 1, 1), email);
    }

    protected PaymentCardRequest cardRequest(String number) {
        return new PaymentCardRequest(number, "IVAN PETROV", LocalDate.of(2030, 12, 31));
    }

    protected Long createUser(String name, String surname, String email) throws Exception {
        Long id = nextUserId();
        String json = mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(id, USER_ROLE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest(name, surname, email))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, UserResponse.class).id();
    }

    protected Long createCard(Long userId, String number) throws Exception {
        String json = mockMvc.perform(post("/api/v1/users/{userId}/cards", userId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId, USER_ROLE))
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
