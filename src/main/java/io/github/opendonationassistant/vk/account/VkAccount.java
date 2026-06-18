package io.github.opendonationassistant.vk.account;

import io.github.opendonationassistant.integration.VkClient;
import io.github.opendonationassistant.integration.VkDataClient;
import io.github.opendonationassistant.vk.reward.Reward;
import io.github.opendonationassistant.vk.reward.RewardData;
import io.github.opendonationassistant.vk.reward.RewardDataRepository;
import io.micronaut.serde.annotation.Serdeable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Serdeable
public class VkAccount {

  private final VkAccountData data;
  private final VkClient vk;
  private final RewardDataRepository rewardRepository;

  public VkAccount(
    VkAccountData data,
    VkClient vk,
    RewardDataRepository rewardRepository
  ) {
    this.data = data;
    this.vk = vk;
    this.rewardRepository = rewardRepository;
  }

  public VkAccountData data() {
    return data;
  }

  public CompletableFuture<Void> acceptReward(
    VkDataClient.AcceptRewardRequest request
  ) {
    return vk.acceptReward(
      data.recipientId(),
      data.refreshTokenId(),
      data.channelUrl(),
      request
    );
  }

  public Stream<Reward> getRewardsForWidget(String widgetId) {
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
    return vk
      .createReward(
        data.recipientId(),
        data.refreshTokenId(),
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

  private Reward convert(RewardData data) {
    return new Reward(data, this, vk);
  }
}
