package io.github.opendonationassistant.integration;

import jakarta.inject.Singleton;

@Singleton
public class VkClient {

  private final VkDataClient client;

  public VkClient(VkDataClient client) {
    this.client = client;
  }

  public void createReward(VkDataClient.RewardRequest request) {
    client.createReward(request);
  }

  public void editReward(VkDataClient.RewardRequest request) {
    client.editReward(request);
  }

  public void deleteReward(String channelId, String rewardId) {
    client.deleteReward(channelId, rewardId);
  }

  public void disableReward(String channelId, String rewardId) {
    client.disableReward(channelId, rewardId);
  }

  public void enableReward(String channelId, String rewardId) {
    client.enableReward(channelId, rewardId);
  }
}
