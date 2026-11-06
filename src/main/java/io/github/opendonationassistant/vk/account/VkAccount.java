package io.github.opendonationassistant.vk.account;

import io.github.opendonationassistant.integration.MediaRPC;
import io.github.opendonationassistant.integration.VkClient;
import io.github.opendonationassistant.integration.VkClientFactory;
import io.github.opendonationassistant.integration.VkDataClient;
import io.github.opendonationassistant.vk.reward.RewardData;
import io.github.opendonationassistant.vk.reward.RewardDataRepository;
import io.github.opendonationassistant.vk.reward.VkReward;
import io.github.opendonationassistant.vk.webhook.VkWebhook;
import io.micronaut.serde.annotation.Serdeable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Serdeable
public class VkAccount {

  public static final String VK_LIVE_SYSTEM = "vklive";

  private final VkAccountData data;
  private final VkClientFactory vkClientFactory;
  private final RewardDataRepository rewardRepository;
  private final MediaRPC media;

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

  public Optional<VkReward> reward(String id) {
    return rewardRepository.findById(id).map(this::convert);
  }

  public Stream<VkReward> getRewardsForWidget(String widgetId) {
    return rewardRepository
      .findByWidgetId(widgetId)
      .stream()
      .map(this::convert);
  }

  public CompletableFuture<Void> createReward(
    String widgetId,
    String type,
    String title,
    Integer cost
  ) {
    return vk()
      .createReward(
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
      )
      .thenAccept(id ->
        rewardRepository.save(new RewardData(id, data.id(), widgetId, type))
      );
  }

  private VkClient vk() {
    return vkClientFactory.create(data.recipientId(), data.refreshTokenId());
  }

  private VkReward convert(RewardData data) {
    return new VkReward(data, this, vkClientFactory, media);
  }
}
