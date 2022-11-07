package cc.towerdefence.api.mcplayerservice.controller;

import cc.towerdefence.api.mcplayerservice.service.McPlayerSecurityService;
import cc.towerdefence.api.service.McPlayerSecurityGrpc;
import cc.towerdefence.api.service.McPlayerSecurityProto;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@GrpcService
@Controller

@RequiredArgsConstructor
public class McPlayerSecurityController extends McPlayerSecurityGrpc.McPlayerSecurityImplBase {
    private final McPlayerSecurityService securityService;

    @Override
    public void addYubiKey(McPlayerSecurityProto.YubikeyRequest request, StreamObserver<McPlayerSecurityProto.YubikeyResponse> responseObserver) {
        this.securityService.addYubikey(UUID.fromString(request.getIssuerId()), request.getOtp())
                .thenAccept(status -> {
                    responseObserver.onNext(McPlayerSecurityProto.YubikeyResponse.newBuilder().setStatus(status).build());
                    responseObserver.onCompleted();
                });
    }

    @Override
    public void verifyYubikey(McPlayerSecurityProto.YubikeyRequest request, StreamObserver<McPlayerSecurityProto.YubikeyResponse> responseObserver) {
        this.securityService.verifyYubikey(UUID.fromString(request.getIssuerId()), request.getOtp())
                .thenAccept(status -> {
                    responseObserver.onNext(McPlayerSecurityProto.YubikeyResponse.newBuilder().setStatus(status).build());
                    responseObserver.onCompleted();
                });
    }
}
