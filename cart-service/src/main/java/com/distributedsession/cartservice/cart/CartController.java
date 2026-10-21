package com.distributedsession.cartservice.cart;

import com.distributedsession.session.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CartController {

    private static final String CART_KEY_PREFIX = "cart-service:cart:";

    private final StringRedisTemplate redisTemplate;

    public CartController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/cart")
    public List<String> viewCart(HttpServletRequest request) {
        HttpSession session = sessionOrThrow(request);
        return redisTemplate.opsForList().range(cartKey(session), 0, -1);
    }

    @PostMapping("/cart/add")
    public List<String> addItem(@Valid @RequestBody AddItemRequest addItemRequest, HttpServletRequest request) {
        HttpSession session = sessionOrThrow(request);
        String key = cartKey(session);
        // RPUSH is a single atomic Redis command, so concurrent adds for the same session
        // landing on different cart-service instances (docker-compose's load balancer has no
        // sticky sessions) can never clobber each other the way a read-modify-write of a whole
        // session attribute could.
        redisTemplate.opsForList().rightPush(key, addItemRequest.item());
        redisTemplate.expire(key, Duration.ofSeconds(session.getMaxInactiveInterval()));
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Unauthorized"));
    }

    private HttpSession sessionOrThrow(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            // SessionAuthenticationFilter already confirmed a valid, authenticated session
            // existed moments ago, but it can be invalidated (e.g. a concurrent /logout, or
            // Redis TTL expiry) before this second, independent getSession(false) lookup runs.
            // Fail with the same generic 401 contract as the filter rather than letting a null
            // session NPE into a 500.
            throw new UnauthorizedException();
        }
        return session;
    }

    private String cartKey(HttpSession session) {
        return CART_KEY_PREFIX + session.getId();
    }

    private static final class UnauthorizedException extends RuntimeException {
    }
}
