package cc.towerdefence.api.mcplayerservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Duration;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Document(collection = "mcPlayer")

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Player {

    @Id
    private UUID id;

    @TextIndexed
    private String currentUsername;

    private Date firstLogin;

    private Date lastOnline;

    @Indexed
    private boolean currentlyOnline;

    private Duration totalPlayTime;

    @TextIndexed
    private Set<String> yubiKeyIdentities;
}
