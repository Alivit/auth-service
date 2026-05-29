package com.minispring.authservice.controller.grpc;

import com.minispring.authservice.service.KeycloakService;
import com.minispring.grpc.AuthGrpcServiceGrpc;
import com.minispring.grpc.DeleteUserRequest;
import com.minispring.grpc.DeleteUserResponse;
import com.minispring.grpc.UserStatusRequest;
import com.minispring.grpc.UserStatusResponse;
import io.grpc.stub.StreamObserver;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.security.access.prepost.PreAuthorize;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class AuthGrpc extends AuthGrpcServiceGrpc.AuthGrpcServiceImplBase {

    private final KeycloakService keycloakService;

    @Override
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public void changeStatus(UserStatusRequest request, StreamObserver<UserStatusResponse> responseObserver) {
        UUID userId = UUID.fromString(request.getUserId());
        boolean enabled = request.getEnabled();

        keycloakService.changeUserStatus(userId, enabled);

        UserStatusResponse response =
                UserStatusResponse.newBuilder().setSuccess(true).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public void deleteUser(DeleteUserRequest request, StreamObserver<DeleteUserResponse> responseObserver) {
        UUID userId = UUID.fromString(request.getUserId());

        keycloakService.deleteUser(userId);

        DeleteUserResponse response =
                DeleteUserResponse.newBuilder().setSuccess(true).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
