package com.salob.food_service.common;

import com.salob.food_service.api._exceptions.EateryNotFoundException;
import io.grpc.Status;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;

@GrpcAdvice
public class GlobalGrpcExceptionHandler {
    @GrpcExceptionHandler(EateryNotFoundException.class)
    public Status handleEateryNotFound(EateryNotFoundException e) {
        return Status.NOT_FOUND.withDescription(e.getMessage()).withCause(e);
    }
}

