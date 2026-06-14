package io.github.opendonationassistant.integration;

import io.github.opendonationassistant.rabbit.TokenRPC;
import io.github.opendonationassistant.rabbit.TokenRPC.TokenRequest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Singleton
public class VkClient {

  private final VkDataClient client;
  private final TokenRPC tokenRPC;

  @Inject
  public VkClient(VkDataClient client, TokenRPC tokenRPC) {
    this.client = client;
    this.tokenRPC = tokenRPC;
  }

  private Optional<String> token(String recipientId, String refreshTokenId) {
    return Optional.ofNullable(
      tokenRPC.token(new TokenRequest(recipientId, refreshTokenId))
    ).map(it -> "Bearer %s".formatted(it.token()));
  }

  public CompletableFuture<String> createReward(
    String recipientId,
    String refreshTokenId,
    String channelUrl,
    VkDataClient.RewardRequest request
  ) {
    return token(recipientId, refreshTokenId)
      .map(token ->
        client
          .createReward(token, channelUrl, request)
          .thenApply(response -> response.data().reward().id())
      )
      .orElse(CompletableFuture.failedFuture(new RuntimeException("No token")));
  }

  public CompletableFuture<Void> editReward(
    String recipientId,
    String refreshTokenId,
    VkDataClient.RewardRequest request
  ) {
    return token(recipientId, refreshTokenId)
      .map(token -> client.editReward(token, request))
      .orElse(CompletableFuture.completedFuture(null));
  }
  // public CompletableFuture<Void> deleteReward(
  //   String channelId,
  //   String rewardId
  // ) {
  //   return client.deleteReward(channelId, rewardId);
  // }
  //
  // public CompletableFuture<Void> disableReward(
  //   String channelId,
  //   String rewardId
  // ) {
  //   client.disableReward(channelId, rewardId);
  // }
  //
  // public CompletableFuture<Void> enableReward(
  //   String channelId,
  //   String rewardId
  // ) {
  //   client.enableReward(channelId, rewardId);
  // }
}
