package com.salob.user_service.common;

import io.grpc.Status;
import io.grpc.StatusException;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;
import org.springframework.stereotype.Component;

@Component
public class GlobalGrpcExceptionHandler implements GrpcExceptionHandler {

    @Override
    public StatusException handleException(Throwable ex) {
        return Status.UNKNOWN.asException();
    }
}
