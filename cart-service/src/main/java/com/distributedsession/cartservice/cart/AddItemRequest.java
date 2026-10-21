package com.distributedsession.cartservice.cart;

import jakarta.validation.constraints.NotBlank;

public record AddItemRequest(@NotBlank String item) {
}
