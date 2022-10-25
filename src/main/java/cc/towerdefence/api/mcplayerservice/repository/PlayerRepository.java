package cc.towerdefence.api.mcplayerservice.repository;

import cc.towerdefence.api.mcplayerservice.model.Player;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerRepository extends MongoRepository<Player, UUID> {

    Optional<Player> findByCurrentUsernameIgnoreCaseOrderById(String username);
}
