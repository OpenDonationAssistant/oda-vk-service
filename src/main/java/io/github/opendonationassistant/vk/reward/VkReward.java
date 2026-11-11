package io.github.opendonationassistant.vk.reward;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.integration.MediaRPC;
import io.github.opendonationassistant.integration.MediaRPC.AddMediaResponse;
import io.github.opendonationassistant.integration.VkClient;
import io.github.opendonationassistant.integration.VkClientFactory;
import io.github.opendonationassistant.integration.VkDataClient;
import io.github.opendonationassistant.vk.account.VkAccount;
import io.github.opendonationassistant.vk.webhook.VkWebhook;
import io.micronaut.serde.annotation.Serdeable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.jspecify.annotations.Nullable;

@Serdeable
public class VkReward {

  private static final int MAX_ERROR_REASON_LENGTH = 200;

  private static final String DEMAND_ID_MISSING_REASON =
    "не удалось определить идентификатор награды";

  private final ODALogger log = new ODALogger(this);
  private final RewardData data;
  private final VkAccount account;
  private final VkClientFactory vkClientFactory;
  private final MediaRPC media;

  public VkReward(
    RewardData data,
    VkAccount account,
    VkClientFactory vkClientFactory,
    MediaRPC media
  ) {
    this.data = data;
    this.account = account;
    this.vkClientFactory = vkClientFactory;
    this.media = media;
  }

  public RewardData data() {
    return data;
  }

  public CompletableFuture<Void> update(String title, Integer cost) {
    return vk()
      .thenCompose(vkClient ->
        vkClient.editReward(
          account.data().channelUrl(),
          data.id(),
          new VkDataClient.RewardRequest(
            new VkDataClient.RewardRequest.Reward(true, title, cost)
          )
        )
      );
  }

  public CompletableFuture<Void> accept(VkWebhook.Demand demand) {
    var user = demand.user();
    var messageParts = demand.messageParts();
    if (user == null || messageParts == null) {
      return CompletableFuture.completedFuture(null);
    }
    return rewardLink(messageParts)
      .map(link -> acceptWithMedia(demand, user, link))
      .orElseGet(() -> CompletableFuture.<Void>completedFuture(null));
  }

  private CompletableFuture<Void> acceptWithMedia(
    VkWebhook.Demand demand,
    VkWebhook.User user,
    String link
  ) {
    if (demand.id() == null) {
      log.error(
        "Cannot process reward demand without id",
        Map.of(
          "rewardId",
          data.id(),
          "recipientId",
          account.data().recipientId()
        )
      );
      return notifyChat(user, DEMAND_ID_MISSING_REASON, false);
    }
    var failureReason = addMedia(mediaId(demand), link, user.nick());
    return failureReason.thenCompose(reason ->
      reason
        .map(it -> declineAndNotify(demand, user, it))
        .orElseGet(() ->
          vk()
            .thenCompose(vkClient ->
              vkClient.acceptReward(
                account.data().channelUrl(),
                new VkDataClient.DemandsRequest(
                  List.of(new VkDataClient.Demand(demand.id()))
                )
              )
            )
        )
    );
  }

  private Optional<String> rewardLink(
    List<Map<String, VkWebhook.Part>> messageParts
  ) {
    return messageParts
      .stream()
      .flatMap(part ->
        part
          .keySet()
          .stream()
          .filter("link"::equals)
          .flatMap(key ->
            Optional.ofNullable(part.get(key))
              .map(VkWebhook.Part::content)
              .stream()
          )
      )
      .findFirst();
  }

  private CompletableFuture<Optional<String>> addMedia(
    String mediaId,
    String link,
    String requester
  ) {
    return media
      .add(
        new MediaRPC.AddMediaRequest(
          mediaId,
          link,
          requester,
          account.data().recipientId(),
          VkAccount.VK_LIVE_SYSTEM
        )
      )
      .handle((response, error) -> {
        if (error != null) {
          return mediaError(error);
        }
        return mediaResult(mediaId, link, response);
      });
  }

  private Optional<String> mediaResult(
    String mediaId,
    String link,
    @Nullable AddMediaResponse response
  ) {
    if (response != null && response.id() != null) {
      return Optional.empty();
    }
    var message = response == null
      ? "нет ответа от сервиса медиа"
      : Objects.toString(response.message(), "");
    log.error(
      "Failed to add media from reward",
      Map.of(
        "mediaId",
        mediaId,
        "url",
        Objects.toString(link, ""),
        "recipientId",
        account.data().recipientId(),
        "error",
        message
      )
    );
    return Optional.of(normalizeReason(message));
  }

  private Optional<String> mediaError(Throwable error) {
    var cause = unwrap(error);
    log.error(
      "Failed to add media from reward",
      cause instanceof Exception exception
        ? exception
        : new CompletionException(cause)
    );
    return Optional.of(normalizeReason(cause.getMessage()));
  }

  private static Throwable unwrap(Throwable error) {
    return error instanceof CompletionException && error.getCause() != null
      ? error.getCause()
      : error;
  }

  // The media service uses this id as a primary key, so the seed is a
  // cross-service contract: it must survive redeliveries and not change.
  private String mediaId(VkWebhook.Demand demand) {
    var accountData = account.data();
    var seed =
      "vk:" +
      accountData.recipientId() +
      ":" +
      accountData.vkId() +
      ":demand:" +
      demand.id();
    return UUID.nameUUIDFromBytes(
      seed.getBytes(StandardCharsets.UTF_8)
    ).toString();
  }

  private CompletableFuture<Void> declineAndNotify(
    VkWebhook.Demand demand,
    VkWebhook.User user,
    String reason
  ) {
    return declineReward(demand).thenCompose(refunded ->
      notifyChat(user, reason, refunded)
    );
  }

  private CompletableFuture<Boolean> declineReward(VkWebhook.Demand demand) {
    try {
      return vk()
        .thenCompose(vkClient ->
          vkClient.rejectReward(
            account.data().channelUrl(),
            new VkDataClient.DemandsRequest(
              List.of(new VkDataClient.Demand(demand.id()))
            )
          )
        )
        .handle((ignored, error) -> {
          if (error != null) {
            logRejectFailure(demand, error);
          }
          return error == null;
        });
    } catch (Exception e) {
      logRejectFailure(demand, e);
      return CompletableFuture.completedFuture(false);
    }
  }

  private void logRejectFailure(VkWebhook.Demand demand, Throwable error) {
    log.error(
      "Failed to reject reward demand",
      Map.of(
        "demandId",
        Objects.toString(demand.id(), ""),
        "recipientId",
        account.data().recipientId(),
        "error",
        Objects.toString(error.getMessage(), "")
      )
    );
  }

  private CompletableFuture<Void> notifyChat(
    VkWebhook.User user,
    String reason,
    boolean refunded
  ) {
    var request = failureMessage(user, reason, refunded);
    try {
      return vk()
        .thenCompose(vkClient ->
          vkClient.sendChatMessage(account.data().channelUrl(), request)
        )
        .exceptionally(e -> {
          logNotifyFailure(e);
          return null;
        });
    } catch (Exception e) {
      logNotifyFailure(e);
      return CompletableFuture.completedFuture(null);
    }
  }

  private void logNotifyFailure(Throwable error) {
    log.error(
      "Failed to notify chat about unfulfilled reward",
      Map.of(
        "rewardId",
        data.id(),
        "recipientId",
        account.data().recipientId(),
        "error",
        Objects.toString(error.getMessage(), "")
      )
    );
  }

  private VkDataClient.ChatMessageRequest failureMessage(
    VkWebhook.User user,
    String reason,
    boolean refunded
  ) {
    var refund = refunded
      ? "Баллы возвращены."
      : "Не удалось автоматически вернуть баллы, напишите стримеру.";
    var text =
      "не удалось добавить медиа по награде: %s. %s".formatted(reason, refund);
    var parts = new ArrayList<VkDataClient.ChatPart>();
    var vkId = user.id();
    if (vkId != null) {
      parts.add(
        new VkDataClient.ChatPart(new VkDataClient.Mention(vkId), null)
      );
      parts.add(
        new VkDataClient.ChatPart(null, new VkDataClient.Text(" " + text))
      );
    } else {
      parts.add(
        new VkDataClient.ChatPart(
          null,
          new VkDataClient.Text("@%s, %s".formatted(user.nick(), text))
        )
      );
    }
    return new VkDataClient.ChatMessageRequest(parts);
  }

  private static String normalizeReason(@Nullable String reason) {
    var value = Objects.toString(reason, "").strip();
    if (value.isEmpty()) {
      return "неизвестная ошибка";
    }
    if (value.length() > MAX_ERROR_REASON_LENGTH) {
      return value.substring(0, MAX_ERROR_REASON_LENGTH) + "…";
    }
    return value;
  }

  private CompletableFuture<VkClient> vk() {
    var accountData = account.data();
    return vkClientFactory.create(
      accountData.recipientId(),
      accountData.refreshTokenId()
    );
  }
}
