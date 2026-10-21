package com.distributedsession.session;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

// Stamps every response with the id of the instance that produced it (ticket 7), so the
// React frontend can show a reader that consecutive requests were answered by different
// instances while the Session and Cart stayed intact.
//
// Display only: nothing routes on this header and no state is keyed by it - the instances
// remain interchangeable, which is the whole point the demo is making.
public class InstanceIdFilter implements Filter {

    public static final String HEADER = "X-Instance-Id";

    private final String instanceId;

    public InstanceIdFilter(String instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // Set before the chain runs: a downstream filter that short-circuits (cart-service's
        // SessionAuthenticationFilter writing a 401) would otherwise commit the response with
        // the header missing - and the 401s are exactly the responses the demo wants labelled.
        ((HttpServletResponse) response).setHeader(HEADER, instanceId);
        chain.doFilter(request, response);
    }
}