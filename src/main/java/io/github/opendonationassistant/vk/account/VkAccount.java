package io.github.opendonationassistant.vk.account;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.integration.MediaRPC;
import io.github.opendonationassistant.integration.VkClient;
import io.github.opendonationassistant.integration.VkClientFactory;
import io.github.opendonationassistant.integration.VkDataClient;
import io.github.opendonationassistant.vk.reward.RewardData;
import io.github.opendonationassistant.vk.reward.RewardDataRepository;
import io.github.opendonationassistant.vk.reward.VkReward;
import io.micronaut.serde.annotation.Serdeable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Serdeable
public class VkAccount {

  public static final String VK_LIVE_SYSTEM = "vklive";

  private final VkAccountData data;
  private final VkClientFactory vkClientFactory;
  private final RewardDataRepository rewardRepository;
  private final MediaRPC media;
  private ODALogger log = new ODALogger(this);

  public VkAccount(
    VkAccountData data,
    VkClientFactory vkClientFactory,
    RewardDataRepository rewardRepository,
    MediaRPC media
  ) {
    this.data = data;
    this.vkClientFactory = vkClientFactory;
    this.rewardRepository = rewardRepository;
    this.media = media;
  }

  public VkAccountData data() {
    return data;
  }

  public CompletableFuture<Optional<VkReward>> reward(String id) {
    return rewardRepository
      .findOneById(id)
      .thenApply(found -> found.map(this::convert));
  }

  public CompletableFuture<List<VkReward>> getRewardsForWidget(
    String widgetId
  ) {
    return rewardRepository
      .findByWidgetId(widgetId)
      .thenApply(rewards -> rewards.stream().map(this::convert).toList());
  }

  public CompletableFuture<Void> createReward(
    String widgetId,
    String type,
    String title,
    Integer cost
  ) {
    return vk()
      .thenCompose(vkClient -> {
        log.debug("Token obtained", Map.of("widgetId", widgetId));
        return vkClient.createReward(
          data.channelUrl(),
          new VkDataClient.RewardRequest(
            new VkDataClient.RewardRequest.Reward(
              // 0,
              // null,
              true,
              // -1,
              // -1,
              title,
              cost
            )
          )
        );
      }
      )
      .thenCompose(id -> {
        log.debug("Reward created, saving it", Map.of("id", id));
        return rewardRepository.save(new RewardData(id, data.id(), widgetId, type));
      })
      .thenAccept(it -> {});
  }

  private CompletableFuture<VkClient> vk() {
    return vkClientFactory.create(data.recipientId(), data.refreshTokenId());
  }

  private VkReward convert(RewardData data) {
    return new VkReward(data, this, vkClientFactory, media);
  }
}
