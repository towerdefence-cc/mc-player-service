package cc.towerdefence.api.mcplayerservice.service;

import cc.towerdefence.api.mcplayerservice.repository.PlayerRepository;
import cc.towerdefence.api.service.McPlayerSecurityProto;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class McPlayerSecurityService {
    private static final String URL = "https://api.yubico.com/wsapi/2.0/verify?id=80802&otp=%s&nonce=%s&timestamp=1";
    private static final char[] NONCE_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    private final PlayerRepository playerRepository;
    private final HttpClient httpClient;

    public CompletableFuture<McPlayerSecurityProto.YubikeyResponse.Status> addYubikey(UUID issuerId, String otp) {
        return this.verifyOtp(otp).thenApply(response -> {
            if (response.status() == McPlayerSecurityProto.YubikeyResponse.Status.OK) {
                this.playerRepository.findById(issuerId).ifPresent(player -> {
                    if (player.getYubiKeyIdentities() == null) {
                        player.setYubiKeyIdentities(Sets.newHashSet(this.parseIdentity(otp)));
                    } else {
                        player.getYubiKeyIdentities().add(otp);
                    }
                    this.playerRepository.save(player);
                });
            }
            return response.status();
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return McPlayerSecurityProto.YubikeyResponse.Status.UNRECOGNIZED;
        });
    }

    public CompletableFuture<McPlayerSecurityProto.YubikeyResponse.Status> verifyYubikey(UUID playerId, String otp) {
        // check records locally first to reduce third party api calls
        String identity = this.parseIdentity(otp);
        if (!this.playerRepository.existsByIdAndYubiKeyIdentitiesContains(playerId, identity))
            return CompletableFuture.completedFuture(McPlayerSecurityProto.YubikeyResponse.Status.KEY_NOT_LINKED);

        // perform request now
        return this.verifyOtp(otp).thenApply(YubicoResponse::status);
    }

    private CompletableFuture<YubicoResponse> verifyOtp(String otp) {
        String nonce = this.generateNonce();
        URI uri = URI.create(URL.formatted(otp, nonce));

        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .build();

        return this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::parseResponse)
                .thenApply(response -> {
                    if (!response.nonce().equals(nonce)) throw new IllegalStateException("Nonce mismatch (original: %s, response: %s)".formatted(nonce, response.nonce()));
                    return response;
                }).exceptionally(throwable -> {
                    throwable.printStackTrace();
                    return null;
                });
    }

    private String parseIdentity(String otp) {
        return otp.substring(0, otp.length() - 32);
    }

    private String generateNonce() {
        Random random = ThreadLocalRandom.current();
        char[] nonce = new char[40];

        for (int i = 0; i < 40; i++) {
            nonce[i] = NONCE_CHARS[random.nextInt(NONCE_CHARS.length)];
        }

        return new String(nonce);
    }

    private YubicoResponse parseResponse(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            return new YubicoResponse(null, null, null,
                    McPlayerSecurityProto.YubikeyResponse.Status.BACKEND_ERROR,
                    -1, -1, 0);
        }

        String body = response.body();

        Map<String, String> values = body.lines()
                .filter(line -> line.contains("="))
                .map(line -> line.split("="))
                .collect(Collectors.toUnmodifiableMap(strings -> strings[0], strings -> strings[1]));

        return new YubicoResponse(
                values.get("otp"),
                values.get("nonce"),
                values.get("h"),
//                Instant.parse(values.get("t")), didnt wanna parse so i removed it
                McPlayerSecurityProto.YubikeyResponse.Status.valueOf(values.get("status")),
                values.containsKey("sessioncounter") ? Long.parseLong(values.get("sessioncounter")) : -1,
                values.containsKey("sessionuse") ? Long.parseLong(values.get("sessionuse")) : -1,
                values.containsKey("sl") ? Integer.parseInt(values.get("sl")) : 0
        );
    }


    // key timestamp is excluded
    private record YubicoResponse(String otp, String nonce, String signature,
                                  McPlayerSecurityProto.YubikeyResponse.Status status,
                                  long sessionCounter, long sessionUse, int sl) {
    }
}
