package com.distributedsession.cartservice.cart;

import com.distributedsession.session.SessionAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CartController {

    @GetMapping("/cart")
    public List<String> viewCart(HttpServletRequest request) {
        return cartFrom(request.getSession(false));
    }

    @PostMapping("/cart/add")
    public List<String> addItem(@Valid @RequestBody AddItemRequest addItemRequest, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        List<String> cart = new ArrayList<>(cartFrom(session));
        cart.add(addItemRequest.item());
        session.setAttribute(SessionAttributes.CART, cart);
        return cart;
    }

    @SuppressWarnings("unchecked")
    private List<String> cartFrom(HttpSession session) {
        List<String> cart = (List<String>) session.getAttribute(SessionAttributes.CART);
        return cart == null ? Collections.emptyList() : cart;
    }
}
