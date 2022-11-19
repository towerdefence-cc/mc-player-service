package cc.towerdefence.api.mcplayerservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document(collection = "mcPlayerUsername")

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PlayerUsername {

    @Id
    private ObjectId id;

    @Indexed
    private UUID playerId;

    @TextIndexed
    private String username;
}
