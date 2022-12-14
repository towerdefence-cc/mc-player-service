package cc.towerdefence.api.mcplayerservice.repository;

import cc.towerdefence.api.mcplayerservice.model.PlayerSession;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerSessionRepository extends MongoRepository<PlayerSession, ObjectId> {

    Page<PlayerSession> findAllByPlayerIdOrderByIdDesc(UUID playerId, Pageable pageable);

    @Query(value = "{ 'playerId' : ?0, 'logoutTime' : null }")
    Optional<PlayerSession> findActiveSessionByPlayerId(UUID playerId);

    /**
     * Used for finding dead sessions of a player
     *
     * @param playerId the player id
     * @return a list of active sessions (where the logout time is not set)
     */
    @Query(value = "{ 'playerId' : ?0, 'logoutTime' : null }")
    List<PlayerSession> findActiveSessionsByPlayerId(UUID playerId);
}
