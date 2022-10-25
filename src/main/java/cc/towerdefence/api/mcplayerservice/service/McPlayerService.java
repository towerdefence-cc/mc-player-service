package cc.towerdefence.api.mcplayerservice.service;

import cc.towerdefence.api.mcplayerservice.model.Player;
import cc.towerdefence.api.mcplayerservice.model.PlayerSession;
import cc.towerdefence.api.mcplayerservice.model.PlayerUsername;
import cc.towerdefence.api.mcplayerservice.repository.PlayerRepository;
import cc.towerdefence.api.mcplayerservice.repository.PlayerSessionRepository;
import cc.towerdefence.api.mcplayerservice.repository.PlayerUsernameRepository;
import cc.towerdefence.api.service.McPlayerProto;
import cc.towerdefence.api.utils.GrpcTimestampConverter;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class McPlayerService {
    private final PlayerRepository playerRepository;
    private final PlayerSessionRepository playerSessionRepository;
    private final PlayerUsernameRepository playerUsernameRepository;

    public Player getPlayer(UUID uuid) {
        return this.playerRepository.findById(uuid).orElse(null);
    }

    public Player getPlayerByUsername(String username) {
        return this.playerRepository.findByCurrentUsernameIgnoreCaseOrderById(username).orElse(null);
    }

    public Page<PlayerSession> getPlayerSessions(UUID playerId, int page) {
        return this.playerSessionRepository.findAllByPlayerIdOrderByIdDesc(playerId, PageRequest.of(page, 10));
    }

    public String onPlayerLogin(McPlayerProto.PlayerLoginRequest request) {
        UUID playerId = UUID.fromString(request.getPlayerId());
        Date date = Date.from(Instant.now());
        Optional<Player> optionalPlayer = this.playerRepository.findById(playerId);

        boolean updatedUsername = false;

        if (optionalPlayer.isEmpty()) {
            Player player = new Player(
                    playerId,
                    request.getUsername(),
                    date,
                    date,
                    true,
                    Duration.ZERO
            );

            this.playerRepository.save(player);
        } else {
            Player player = optionalPlayer.get();
            player.setCurrentUsername(request.getUsername());
            player.setCurrentlyOnline(true);
            this.playerRepository.save(player);
        }

        PlayerSession session = new PlayerSession(new ObjectId(date), playerId, null);
        this.playerSessionRepository.save(session);

        if (updatedUsername)
            this.playerUsernameRepository.save(new PlayerUsername(new ObjectId(date), playerId, request.getUsername()));

        return session.getId().toHexString();
    }

    public void onPlayerDisconnect(McPlayerProto.PlayerDisconnectRequest request) {
        UUID playerId = UUID.fromString(request.getPlayerId());

        PlayerSession session = this.playerSessionRepository.findById(new ObjectId(request.getSessionId())).orElseThrow();
        session.setLogoutTime(Date.from(Instant.now()));
        this.playerSessionRepository.save(session);

        Player player = this.playerRepository.findById(playerId).orElseThrow();

        player.setCurrentlyOnline(false);
        player.setLastOnline(Date.from(Instant.now()));
        player.setTotalPlayTime(player.getTotalPlayTime().plus(session.getDuration())); // todo this seems to add the wrong amount
        this.playerRepository.save(player);
    }
}
