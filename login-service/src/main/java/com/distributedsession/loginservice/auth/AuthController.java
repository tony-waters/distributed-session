package com.distributedsession.loginservice.auth;

import com.distributedsession.session.ErrorResponse;
import com.distributedsession.session.SessionAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthenticationManager authenticationManager;

    public AuthController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException e) {
            // Unknown username and wrong password return the identical generic body: no detail
            // that would let a caller enumerate valid usernames (ADR-0003).
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid username or password"));
        }

        // Login always mints a brand-new session (GLOSSARY.md: Login), never reuses one the
        // caller already presented - that also prevents session fixation, since a pre-set
        // SESSION cookie can never become authenticated as whoever logs in with it.
        HttpSession existingSession = httpRequest.getSession(false);
        if (existingSession != null) {
            existingSession.invalidate();
        }
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(SessionAttributes.USERNAME, request.username());
        return ResponseEntity.ok().build();
    }

    // Lets a browser frontend recover who it is logged in as after a page reload, since the
    // SESSION cookie is HttpOnly and the username only ever lives in the Session (ticket 7).
    // Reports the same generic 401 as cart-service for a missing, unknown or half-built
    // session: no detail about which it was.
    @GetMapping("/session")
    public ResponseEntity<?> currentSession(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        String username = session == null
                ? null
                : (String) session.getAttribute(SessionAttributes.USERNAME);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Unauthorized"));
        }
        return ResponseEntity.ok(new SessionResponse(username));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok().build();
    }
}
