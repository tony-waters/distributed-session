package com.distributedsession.loginservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.distributedsession.session.ErrorResponse;
import com.distributedsession.loginservice.auth.LoginRequest;
import com.distributedsession.loginservice.auth.SessionResponse;
import com.distributedsession.session.InstanceIdFilter;
import com.distributedsession.session.SessionAttributes;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "INSTANCE_ID=login-test")
@AutoConfigureTestRestTemplate
@Testcontainers
class LoginServiceApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

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
    void successfulLoginYieldsUsableSessionCookie() {
        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/login", new LoginRequest("alice", "alice-password123"), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotNull().contains("SESSION=");

        Session session = sessionRepository.findById(decodeSessionId(setCookie));
        assertThat(session).isNotNull();
        assertThat(session.<String>getAttribute(SessionAttributes.USERNAME)).isEqualTo("alice");
    }

    @Test
    void unknownUsernameAndWrongPasswordReturnIdenticalGenericUnauthorized() {
        ErrorResponse expectedBody = new ErrorResponse("Invalid username or password");

        ResponseEntity<ErrorResponse> unknownUsername = restTemplate.postForEntity(
                "/login", new LoginRequest("nobody", "whatever"), ErrorResponse.class);
        ResponseEntity<ErrorResponse> wrongPassword = restTemplate.postForEntity(
                "/login", new LoginRequest("alice", "wrong-password"), ErrorResponse.class);

        assertThat(unknownUsername.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unknownUsername.getBody()).isEqualTo(expectedBody);
        assertThat(wrongPassword.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(wrongPassword.getBody()).isEqualTo(expectedBody);
    }

    @Test
    void loginAlwaysMintsAFreshSessionEvenWithAnExistingCookie() {
        ResponseEntity<Void> firstLogin = restTemplate.postForEntity(
                "/login", new LoginRequest("alice", "alice-password123"), Void.class);
        String firstCookie = firstLogin.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        String firstSessionId = decodeSessionId(firstCookie);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, firstCookie.split(";")[0]);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Void> secondLogin = restTemplate.exchange("/login", HttpMethod.POST,
                new HttpEntity<>(new LoginRequest("alice", "alice-password123"), headers), Void.class);
        String secondCookie = secondLogin.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        String secondSessionId = decodeSessionId(secondCookie);

        assertThat(secondSessionId).isNotEqualTo(firstSessionId);
        assertThat(sessionRepository.findById(firstSessionId)).isNull();
        assertThat(sessionRepository.findById(secondSessionId)).isNotNull();
    }

    @Test
    void logoutInvalidatesTheSession() {
        ResponseEntity<Void> loginResponse = restTemplate.postForEntity(
                "/login", new LoginRequest("bob", "bob-password123"), Void.class);
        String setCookie = loginResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        String sessionId = decodeSessionId(setCookie);
        assertThat(sessionRepository.findById(sessionId)).isNotNull();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, setCookie.split(";")[0]);
        ResponseEntity<Void> logoutResponse = restTemplate.exchange(
                "/logout", HttpMethod.POST, new HttpEntity<>(headers), Void.class);

        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sessionRepository.findById(sessionId)).isNull();
    }

    @Test
    void currentSessionReportsTheLoggedInUsername() {
        ResponseEntity<Void> loginResponse = restTemplate.postForEntity(
                "/login", new LoginRequest("alice", "alice-password123"), Void.class);
        String setCookie = loginResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, setCookie.split(";")[0]);
        ResponseEntity<SessionResponse> response = restTemplate.exchange(
                "/session", HttpMethod.GET, new HttpEntity<>(headers), SessionResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new SessionResponse("alice"));
    }

    @Test
    void currentSessionWithoutASessionIsRejected() {
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity("/session", ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo(new ErrorResponse("Unauthorized"));
    }

    @Test
    void everyResponseIsLabelledWithTheInstanceThatAnsweredIt() {
        ResponseEntity<ErrorResponse> unauthorized = restTemplate.getForEntity("/session", ErrorResponse.class);
        ResponseEntity<Void> ok = restTemplate.postForEntity(
                "/login", new LoginRequest("alice", "alice-password123"), Void.class);

        assertThat(unauthorized.getHeaders().getFirst(InstanceIdFilter.HEADER)).isEqualTo("login-test");
        assertThat(ok.getHeaders().getFirst(InstanceIdFilter.HEADER)).isEqualTo("login-test");
    }

    private static String decodeSessionId(String setCookieHeader) {
        String rawCookieValue = setCookieHeader.split(";")[0].split("=", 2)[1];
        return new String(Base64.getDecoder().decode(rawCookieValue));
    }
}
