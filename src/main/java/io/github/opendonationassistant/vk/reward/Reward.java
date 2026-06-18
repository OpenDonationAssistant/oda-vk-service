package io.github.opendonationassistant.vk.reward;

import java.util.concurrent.CompletableFuture;

import io.github.opendonationassistant.integration.VkClient;
import io.github.opendonationassistant.integration.VkDataClient;
import io.github.opendonationassistant.vk.account.VkAccount;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class Reward {

  private final RewardData data;
  private final VkAccount account;
  private final VkClient vk;

  public Reward(RewardData data, VkAccount account, VkClient vk) {
    this.data = data;
    this.account = account;
    this.vk = vk;
  }

  public RewardData data() {
    return data;
  }

  public CompletableFuture<Void> update(String title, Integer cost) {
    return vk.editReward(
      account.data().recipientId(),
      account.data().refreshTokenId(),
      account.data().channelUrl(),
      data.id(),
      new VkDataClient.RewardRequest(
        new VkDataClient.RewardRequest.Reward(true, title, cost)
      )
    );
  }
}
