package com.distributedsession.session;

// Names of the attributes login-service writes into the shared Redis-backed
// HttpSession and cart-service reads back (see GLOSSARY.md: Session).
public final class SessionAttributes {

    public static final String USERNAME = "username";

    private SessionAttributes() {
    }
}
