package cc.towerdefence.api.mcplayerservice.repository;

import cc.towerdefence.api.mcplayerservice.model.Player;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerRepository extends MongoRepository<Player, UUID> {

    Optional<Player> findByCurrentUsernameIgnoreCaseOrderById(String username);

    Page<Player> findAllByCurrentUsernameIgnoreCaseOrderById(String username, Pageable pageable);

    Page<Player> findAllByCurrentUsernameAndCurrentlyOnlineOrderById(String username, boolean online, Pageable pageable);

    boolean existsByIdAndYubiKeyIdentitiesContains(UUID id, String yubiKeyId);
}
