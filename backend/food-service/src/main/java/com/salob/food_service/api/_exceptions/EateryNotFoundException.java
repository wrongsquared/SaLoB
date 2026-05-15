package com.salob.food_service.api._exceptions;

import java.util.UUID;

public class EateryNotFoundException extends RuntimeException {
    public EateryNotFoundException(UUID id) {
        super("Eatery of ID: " + id + " not found");
    }
}
