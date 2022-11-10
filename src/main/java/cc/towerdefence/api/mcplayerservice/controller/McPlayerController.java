package cc.towerdefence.api.mcplayerservice.controller;

import cc.towerdefence.api.mcplayerservice.model.Player;
import cc.towerdefence.api.mcplayerservice.model.PlayerSession;
import cc.towerdefence.api.mcplayerservice.service.McPlayerService;
import cc.towerdefence.api.model.PlayerProto;
import cc.towerdefence.api.service.McPlayerGrpc;
import cc.towerdefence.api.service.McPlayerProto;
import cc.towerdefence.api.utils.GrpcDurationConverter;
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
    public void getPlayer(PlayerProto.PlayerRequest request, StreamObserver<McPlayerProto.PlayerResponse> responseObserver) {
        McPlayerProto.PlayerResponse response = this.mcPlayerService.getPlayer(UUID.fromString(request.getPlayerId()));

        if (response == null) {
            responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
            return;
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getPlayers(PlayerProto.PlayersRequest request, StreamObserver<McPlayerProto.PlayersResponse> responseObserver) {
        List<McPlayerProto.PlayerResponse> players = this.mcPlayerService.getPlayers(request.getPlayerIdsList().stream().map(UUID::fromString).toList());

        responseObserver.onNext(McPlayerProto.PlayersResponse.newBuilder().addAllPlayers(players).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getPlayerByUsername(PlayerProto.PlayerUsernameRequest request, StreamObserver<McPlayerProto.PlayerResponse> responseObserver) {
        McPlayerProto.PlayerResponse response = this.mcPlayerService.getPlayerByUsername(request.getUsername());

        if (response == null) {
            responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
            return;
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void searchPlayersByUsername(McPlayerProto.PlayerSearchRequest request, StreamObserver<McPlayerProto.PlayerSearchResponse> responseObserver) {
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

    private McPlayerProto.PlayerResponse convertPlayer(Player player) {
        return McPlayerProto.PlayerResponse.newBuilder()
                .setId(player.getId().toString())
                .setCurrentUsername(player.getCurrentUsername())
                .setFirstLogin(GrpcTimestampConverter.convert(player.getFirstLogin().toInstant()))
                .setLastOnline(GrpcTimestampConverter.convert(player.getLastOnline().toInstant()))
                .setCurrentlyOnline(player.isCurrentlyOnline())
                .setPlayTime(GrpcDurationConverter.convert(player.getTotalPlayTime()))
                .setOtpEnabled(player.getYubiKeyIdentities() != null && !player.getYubiKeyIdentities().isEmpty())
                .build();
    }

    @Override
    public void getPlayerSessions(McPlayerProto.PageablePlayerRequest request, StreamObserver<McPlayerProto.PlayerSessionsResponse> responseObserver) {
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
    public void onPlayerLogin(McPlayerProto.PlayerLoginRequest request, StreamObserver<McPlayerProto.PlayerLoginResponse> responseObserver) {
        String sessionId = this.mcPlayerService.onPlayerLogin(request);
        responseObserver.onNext(McPlayerProto.PlayerLoginResponse.newBuilder().setSessionId(sessionId).build());
        responseObserver.onCompleted();
    }

    @Override
    public void onPlayerDisconnect(McPlayerProto.PlayerDisconnectRequest request, StreamObserver<Empty> responseObserver) {
        this.mcPlayerService.onPlayerDisconnect(request);
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
