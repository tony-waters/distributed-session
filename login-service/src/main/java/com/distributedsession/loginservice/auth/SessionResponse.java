package com.distributedsession.loginservice.auth;

// Body of GET /session: the username held by the caller's Session. Deliberately not the
// session ID - that stays in the HttpOnly cookie and is never handed to page scripts.
public record SessionResponse(String username) {
}
