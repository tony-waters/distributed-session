package com.distributedsession.cartservice.cart;

import com.distributedsession.session.ErrorResponse;
import com.distributedsession.session.SessionAttributes;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

// Runs before every cart endpoint so a new endpoint can't accidentally skip
// authentication (ticket 4): rejects any request without a session that carries
// the username login-service writes, with the same generic 401 regardless of
// whether the session is missing, unknown, or just lacks that attribute.
//
// Registered (see FilterConfig) only for /cart/* - unscoped it would default to
// "/*" and also reject the unauthenticated /actuator/health probe docker-compose's
// healthcheck depends on.
public class SessionAuthenticationFilter implements Filter {

    private final ObjectMapper objectMapper;

    public SessionAuthenticationFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute(SessionAttributes.USERNAME) == null) {
            httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(httpResponse.getWriter(), new ErrorResponse("Unauthorized"));
            return;
        }

        chain.doFilter(request, response);
    }
}
