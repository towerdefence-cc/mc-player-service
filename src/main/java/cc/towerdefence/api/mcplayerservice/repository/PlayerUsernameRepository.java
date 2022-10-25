package cc.towerdefence.api.mcplayerservice.repository;

import cc.towerdefence.api.mcplayerservice.model.PlayerUsername;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerUsernameRepository extends MongoRepository<PlayerUsername, ObjectId> {
}
