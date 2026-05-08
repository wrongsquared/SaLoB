package com.salob.food_service.common;

import com.salob.food_service.features.eatery.exceptions.EateryNotFoundException;
import io.grpc.Status;
import io.grpc.StatusException;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;
import org.springframework.stereotype.Component;

@Component
public class GlobalGrpcExceptionHandler implements GrpcExceptionHandler {

    @Override
    public StatusException handleException(Throwable ex) {
        if (ex instanceof EateryNotFoundException) {
            return Status.NOT_FOUND
                    .withDescription(ex.getMessage())
                    .asException();
        }
        return Status.INTERNAL.asException();
    }
}

