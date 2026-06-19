package com.minispring.authservice.controller.grpc;

import com.minispring.authservice.exception.ResourceNotFoundException;
import com.minispring.authservice.service.KeycloakService;
import com.minispring.grpc.AuthGrpcServiceGrpc;
import com.minispring.grpc.UserStatusRequest;
import com.minispring.grpc.UserStatusResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuthGrpc extends AuthGrpcServiceGrpc.AuthGrpcServiceImplBase{

    private final KeycloakService keycloakService;

    @Override
    public void changeStatus(UserStatusRequest request, StreamObserver<UserStatusResponse> responseObserver) {
        try {
            UUID userId = UUID.fromString(request.getUserId());
            boolean enabled = request.getEnabled();

            keycloakService.changeUserStatus(userId, enabled);

            UserStatusResponse response = UserStatusResponse.newBuilder()
                    .setSuccess(true)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (ResourceNotFoundException ex) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(ex.getMessage()).asRuntimeException());
        } catch (IllegalArgumentException ex) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid UUID format").asRuntimeException());
        } catch (Exception ex) {
            responseObserver.onError(Status.INTERNAL.withDescription("Internal server error").withCause(ex)
                    .asRuntimeException());
        }
    }
}
