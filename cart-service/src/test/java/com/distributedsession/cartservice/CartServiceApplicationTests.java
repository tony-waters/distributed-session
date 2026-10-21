package com.distributedsession.cartservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.distributedsession.cartservice.cart.AddItemRequest;
import com.distributedsession.session.ErrorResponse;
import com.distributedsession.session.SessionAttributes;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
class CartServiceApplicationTests {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    @SuppressWarnings("rawtypes")
    private SessionRepository sessionRepository;

    @Test
    void requestWithNoSessionCookieIsRejected() {
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity("/cart", ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo(new ErrorResponse("Unauthorized"));
    }

    @Test
    void unknownSessionIdIsRejected() {
        HttpHeaders headers = cookieHeaders(Base64.getEncoder().encodeToString("not-a-real-session-id".getBytes()));
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/cart", HttpMethod.GET, new HttpEntity<>(headers), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo(new ErrorResponse("Unauthorized"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sessionMissingUsernameAttributeIsRejected() {
        Session session = sessionRepository.createSession();
        sessionRepository.save(session);

        HttpHeaders headers = cookieHeaders(Base64.getEncoder().encodeToString(session.getId().getBytes()));
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/cart", HttpMethod.GET, new HttpEntity<>(headers), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo(new ErrorResponse("Unauthorized"));
    }

    @Test
    void validSessionWithNoCartYetSeesAnEmptyCart() {
        HttpHeaders headers = cookieHeaders(preSeedSessionFor("alice"));
        ResponseEntity<List> response = restTemplate.exchange(
                "/cart", HttpMethod.GET, new HttpEntity<>(headers), List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void addingItemsAppendsToTheCartIncludingDuplicates() {
        HttpHeaders headers = cookieHeaders(preSeedSessionFor("alice"));
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<List> first = restTemplate.exchange(
                "/cart/add", HttpMethod.POST, new HttpEntity<>(new AddItemRequest("widget"), headers), List.class);
        ResponseEntity<List> second = restTemplate.exchange(
                "/cart/add", HttpMethod.POST, new HttpEntity<>(new AddItemRequest("widget"), headers), List.class);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(first.getBody()).isEqualTo(List.of("widget"));
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getBody()).isEqualTo(List.of("widget", "widget"));
    }

    @Test
    void addingABlankItemIsRejected() {
        HttpHeaders headers = cookieHeaders(preSeedSessionFor("alice"));
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                "/cart/add", HttpMethod.POST, new HttpEntity<>(new AddItemRequest(" "), headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @SuppressWarnings("unchecked")
    private String preSeedSessionFor(String username) {
        Session session = sessionRepository.createSession();
        session.setAttribute(SessionAttributes.USERNAME, username);
        sessionRepository.save(session);
        return Base64.getEncoder().encodeToString(session.getId().getBytes());
    }

    private static HttpHeaders cookieHeaders(String sessionCookieValue) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "SESSION=" + sessionCookieValue);
        return headers;
    }
}
