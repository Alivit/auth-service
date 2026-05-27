package com.minispring.authservice.service.impl;

import com.minispring.authservice.exception.ResourceNotFoundException;
import com.minispring.grpc.AuthGrpcServiceGrpc;
import com.minispring.grpc.UserStatusRequest;
import com.minispring.grpc.UserStatusResponse;
import io.grpc.stub.StreamObserver;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.grpc.server.service.GrpcService;

import java.util.UUID;

import static com.minispring.authservice.exception.ExceptionAnswer.USER_NOT_FOUND;

@GrpcService
@RequiredArgsConstructor
public class AuthGrpcService extends AuthGrpcServiceGrpc.AuthGrpcServiceImplBase{
    private final Keycloak keycloakClient;

    @Value("${keycloak.realm}")
    private String realm;

    @Override
    public void changeStatus(UserStatusRequest request, StreamObserver<UserStatusResponse> responseObserver) {
        try {
            UUID userId = UUID.fromString(request.getUserId());
            boolean enabled = request.getEnabled();

            UserResource userResource = keycloakClient.realm(realm).users().get(userId.toString());
            UserRepresentation userRepresentation = userResource.toRepresentation();

            userRepresentation.setEnabled(enabled);
            userResource.update(userRepresentation);

            if (!enabled) {
                userResource.logout();
            }

            UserStatusResponse response = UserStatusResponse.newBuilder()
                    .setSuccess(true)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (NotFoundException e) {
            throw new ResourceNotFoundException(String.format(USER_NOT_FOUND, request.getUserId()));
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == 404) {
                throw new ResourceNotFoundException(String.format(USER_NOT_FOUND, request.getUserId()));
            }
            throw new RuntimeException("Keycloak API failure: HTTP " + e.getResponse().getStatus());
        }
    }
}
