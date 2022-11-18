package cc.towerdefence.api.mcplayerservice.service;

import cc.towerdefence.api.mcplayerservice.model.Player;
import cc.towerdefence.api.mcplayerservice.model.PlayerSession;
import cc.towerdefence.api.mcplayerservice.model.PlayerUsername;
import cc.towerdefence.api.mcplayerservice.repository.PlayerRepository;
import cc.towerdefence.api.mcplayerservice.repository.PlayerSessionRepository;
import cc.towerdefence.api.mcplayerservice.repository.PlayerUsernameRepository;
import cc.towerdefence.api.service.McPlayerProto;
import cc.towerdefence.api.utils.GrpcDurationConverter;
import cc.towerdefence.api.utils.GrpcTimestampConverter;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class McPlayerService {
    private final PlayerRepository playerRepository;
    private final PlayerSessionRepository playerSessionRepository;
    private final PlayerUsernameRepository playerUsernameRepository;

    public McPlayerProto.PlayerResponse getPlayer(UUID uuid) {
        return this.playerRepository.findById(uuid).map(this::convertPlayer).orElse(null);
    }

    public List<McPlayerProto.PlayerResponse> getPlayers(List<UUID> uuids) {
        return StreamSupport.stream(this.playerRepository.findAllById(uuids).spliterator(), true)
                .map(this::convertPlayer)
                .toList();
    }

    public McPlayerProto.PlayerResponse getPlayerByUsername(String username) {
        Optional<Player> optionalPlayer = this.playerRepository.findByCurrentUsernameIgnoreCaseOrderById(username);
        if (optionalPlayer.isEmpty()) return null;

        Player player = optionalPlayer.get();

        return this.convertPlayer(player);
    }

    public Page<McPlayerProto.PlayerResponse> searchPlayerByUsername(McPlayerProto.McPlayerSearchRequest request) {
        String username = request.getSearchUsername();
        Pageable pageable = PageRequest.of(request.getPage(), request.getPageSize());
        McPlayerProto.McPlayerSearchRequest.FilterMethod filterMethod = request.getFilterMethod();

        // todo implement friend methods
        Page<Player> page = switch (filterMethod) {
            case NONE -> this.playerRepository.findAllByCurrentUsernameIgnoreCaseOrderById(username, pageable);
            case ONLINE -> this.playerRepository.findAllByCurrentUsernameAndCurrentlyOnlineOrderById(username, true, pageable);
            case FRIENDS -> throw new UnsupportedOperationException("Not implemented yet");
            case UNRECOGNIZED -> throw new UnsupportedOperationException("Unrecognized filter method");
        };

        return page.map(this::convertPlayer);

    }

    public Page<PlayerSession> getPlayerSessions(UUID playerId, int page) {
        return this.playerSessionRepository.findAllByPlayerIdOrderByIdDesc(playerId, PageRequest.of(page, 10));
    }

    public String onPlayerLogin(McPlayerProto.McPlayerLoginRequest request) {
        UUID playerId = UUID.fromString(request.getPlayerId());
        Date date = Date.from(Instant.now());
        Optional<Player> optionalPlayer = this.playerRepository.findById(playerId);

        boolean updatedUsername = optionalPlayer.isEmpty() || !optionalPlayer.get().getCurrentUsername().equals(request.getUsername());

        Player player;
        if (optionalPlayer.isEmpty()) {
            player = new Player(
                    playerId,
                    request.getUsername(),
                    date,
                    date,
                    true,
                    Duration.ZERO,
                    new HashSet<>()
            );
        } else {
            player = optionalPlayer.get();
            player.setCurrentUsername(request.getUsername());
            player.setCurrentlyOnline(true);
        }

        this.playerRepository.save(player);

        PlayerSession session = new PlayerSession(new ObjectId(date), playerId, null);
        this.playerSessionRepository.save(session);

        if (updatedUsername)
            this.playerUsernameRepository.save(new PlayerUsername(new ObjectId(date), playerId, request.getUsername()));

        return session.getId().toHexString();
    }

    public void onPlayerDisconnect(McPlayerProto.McPlayerDisconnectRequest request) {
        UUID playerId = UUID.fromString(request.getPlayerId());

        PlayerSession session = this.playerSessionRepository.findById(new ObjectId(request.getSessionId())).orElseThrow();
        session.setLogoutTime(Date.from(Instant.now()));
        this.playerSessionRepository.save(session);

        Player player = this.playerRepository.findById(playerId).orElseThrow();

        player.setCurrentlyOnline(false);
        player.setLastOnline(Date.from(Instant.now()));
        player.setTotalPlayTime(player.getTotalPlayTime().plus(session.getDuration()));
        this.playerRepository.save(player);
    }

    private McPlayerProto.PlayerSession convertSession(PlayerSession session) {
        McPlayerProto.PlayerSession.Builder builder = McPlayerProto.PlayerSession.newBuilder()
                .setSessionId(session.getId().toString())
                .setLoginTime(GrpcTimestampConverter.convertMillis(session.getId().getTimestamp() * 1000L));

        if (session.getLogoutTime() != null)
                builder.setLogoutTime(GrpcTimestampConverter.convert(session.getLogoutTime().toInstant()));

        return builder.build();
    }

    private McPlayerProto.PlayerResponse convertPlayer(Player player) {
        McPlayerProto.PlayerResponse.Builder builder =  McPlayerProto.PlayerResponse.newBuilder()
                .setId(player.getId().toString())
                .setCurrentUsername(player.getCurrentUsername())
                .setFirstLogin(GrpcTimestampConverter.convert(player.getFirstLogin().toInstant()))
                .setLastOnline(GrpcTimestampConverter.convert(player.getLastOnline().toInstant()))
                .setCurrentlyOnline(player.isCurrentlyOnline())
                .setPlayTime(GrpcDurationConverter.convert(player.getTotalPlayTime()))
                .setOtpEnabled(player.getYubiKeyIdentities() != null && !player.getYubiKeyIdentities().isEmpty());

        if (player.isCurrentlyOnline()) {
            this.playerSessionRepository.findActiveSessionByPlayerId(player.getId())
                    .ifPresent(session -> builder.setCurrentSession(this.convertSession(session)));
        }

        return builder.build();
    }
}
