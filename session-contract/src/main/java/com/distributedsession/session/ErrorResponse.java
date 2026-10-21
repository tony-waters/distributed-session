package com.distributedsession.session;

// Shared generic-error body: both login-service and cart-service return this shape
// for any auth failure, deliberately uninformative about which part failed.
public record ErrorResponse(String error) {
}
