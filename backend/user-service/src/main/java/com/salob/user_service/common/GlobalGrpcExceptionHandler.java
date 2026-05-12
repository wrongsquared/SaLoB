package com.salob.user_service.common;

import io.grpc.Status;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;

@GrpcAdvice
public class GlobalGrpcExceptionHandler {

    @GrpcExceptionHandler(Exception.class)
    public Status handleException(Exception e) {
        return Status.UNKNOWN.withDescription(e.getMessage()).withCause(e);
    }
}
