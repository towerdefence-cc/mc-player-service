package cc.towerdefence.api.mcplayerservice.controller;

import cc.towerdefence.api.mcplayerservice.model.PlayerSession;
import cc.towerdefence.api.mcplayerservice.service.McPlayerService;
import cc.towerdefence.api.service.McPlayerGrpc;
import cc.towerdefence.api.service.McPlayerProto;
import cc.towerdefence.api.utils.GrpcTimestampConverter;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@GrpcService
@Controller

@RequiredArgsConstructor
public class McPlayerController extends McPlayerGrpc.McPlayerImplBase {
    private final McPlayerService mcPlayerService;

    @Override
    public void getPlayer(McPlayerProto.PlayerRequest request, StreamObserver<McPlayerProto.PlayerResponse> responseObserver) {
        McPlayerProto.PlayerResponse response = this.mcPlayerService.getPlayer(UUID.fromString(request.getPlayerId()));

        if (response == null) {
            responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
            return;
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getPlayers(McPlayerProto.PlayersRequest request, StreamObserver<McPlayerProto.PlayersResponse> responseObserver) {
        List<McPlayerProto.PlayerResponse> players = this.mcPlayerService.getPlayers(request.getPlayerIdsList().stream().map(UUID::fromString).toList());

        responseObserver.onNext(McPlayerProto.PlayersResponse.newBuilder().addAllPlayers(players).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getPlayerByUsername(McPlayerProto.PlayerUsernameRequest request, StreamObserver<McPlayerProto.PlayerResponse> responseObserver) {
        McPlayerProto.PlayerResponse response = this.mcPlayerService.getPlayerByUsername(request.getUsername());

        if (response == null) {
            responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
            return;
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void searchPlayersByUsername(McPlayerProto.McPlayerSearchRequest request, StreamObserver<McPlayerProto.PlayerSearchResponse> responseObserver) {
        Page<McPlayerProto.PlayerResponse> playerPage = this.mcPlayerService.searchPlayerByUsername(request);

        responseObserver.onNext(
                McPlayerProto.PlayerSearchResponse.newBuilder()
                        .addAllPlayers(playerPage.getContent())
                        .setPage(playerPage.getNumber())
                        .setTotalPages(playerPage.getTotalPages())
                        .setTotalElements((int) playerPage.getTotalElements())
                        .build()
        );
        responseObserver.onCompleted();
    }

    @Override
    public void getPlayerSessions(McPlayerProto.McPageablePlayerRequest request, StreamObserver<McPlayerProto.PlayerSessionsResponse> responseObserver) {
        Page<PlayerSession> sessionsPage = this.mcPlayerService.getPlayerSessions(UUID.fromString(request.getPlayerId()), request.getPage());

        List<McPlayerProto.PlayerSession> sessions = sessionsPage
                .map(session -> McPlayerProto.PlayerSession.newBuilder()
                        .setSessionId(session.getId().toString())
                        .setLoginTime(GrpcTimestampConverter.convertMillis(session.getId().getTimestamp() * 1000L))
                        .setLogoutTime(GrpcTimestampConverter.convert(session.getLogoutTime().toInstant()))
                        .build())
                .getContent();

        responseObserver.onNext(McPlayerProto.PlayerSessionsResponse.newBuilder()
                .setPage(sessionsPage.getNumber())
                .setTotalElements(sessionsPage.getTotalElements())
                .setTotalPages(sessionsPage.getTotalPages())
                .addAllSessions(sessions).build());
        responseObserver.onCompleted();
    }

    @Override
    public void onPlayerLogin(McPlayerProto.McPlayerLoginRequest request, StreamObserver<McPlayerProto.PlayerLoginResponse> responseObserver) {
        String sessionId = this.mcPlayerService.onPlayerLogin(request);
        responseObserver.onNext(McPlayerProto.PlayerLoginResponse.newBuilder().setSessionId(sessionId).build());
        responseObserver.onCompleted();
    }

    @Override
    public void onPlayerDisconnect(McPlayerProto.McPlayerDisconnectRequest request, StreamObserver<Empty> responseObserver) {
        this.mcPlayerService.onPlayerDisconnect(request);
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
