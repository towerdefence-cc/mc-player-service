package cc.towerdefence.api.mcplayerservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Document(collection = "mcPlayerSession")

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PlayerSession {

    @Id
    private ObjectId id;

    @Indexed
    private UUID playerId;

    private Date logoutTime;

    public Duration getDuration() {
        return Duration.between(this.id.getDate().toInstant(), this.logoutTime.toInstant());
    }
}
