package io.github.opendonationassistant.integration;

import io.github.opendonationassistant.integration.VkDataClient.DataWrapper;
import io.github.opendonationassistant.integration.VkDataClient.Stream;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class VkClient {

  private final VkDataClient client;
  private final String token;

  public VkClient(VkDataClient client, String token) {
    this.client = Objects.requireNonNull(client, "client");
    this.token = Objects.requireNonNull(token, "token");
  }

  public CompletableFuture<String> createReward(
    String channelUrl,
    VkDataClient.RewardRequest request
  ) {
    return client
      .createReward(token, channelUrl, request)
      .thenApply(response -> response.data().reward().id());
  }

  public CompletableFuture<Void> acceptReward(
    String channelUrl,
    VkDataClient.DemandsRequest request
  ) {
    return client.acceptReward(token, channelUrl, request);
  }

  public CompletableFuture<Void> rejectReward(
    String channelUrl,
    VkDataClient.DemandsRequest request
  ) {
    return client.rejectReward(token, channelUrl, request);
  }

  public CompletableFuture<Void> sendChatMessage(
    String channelUrl,
    VkDataClient.ChatMessageRequest request
  ) {
    return client
      .getChannelInfo(token, channelUrl)
      .thenCompose(response ->
        Optional.ofNullable(response)
          .map(DataWrapper::data)
          .flatMap(VkClient::activeStreamId)
          .map(streamId ->
            client.sendChatMessage(token, channelUrl, streamId, request)
          )
          .orElseGet(() ->
            CompletableFuture.failedFuture(
              new RuntimeException(
                "Channel %s has no active stream".formatted(channelUrl)
              )
            )
          )
      );
  }

  private static Optional<String> activeStreamId(
    VkDataClient.ChannelInfo info
  ) {
    return Optional.ofNullable(info.stream())
      .map(Stream::id)
      .or(() ->
        Optional.ofNullable(info.streams())
          .flatMap(streams ->
            streams.stream().map(Stream::id).findFirst()
          )
      );
  }

  public CompletableFuture<Void> editReward(
    String channelUrl,
    String rewardId,
    VkDataClient.RewardRequest request
  ) {
    return client.editReward(token, channelUrl, rewardId, request);
  }

  public CompletableFuture<VkDataClient.ChannelInfo> getChannelInfo(
    String channelUrl
  ) {
    return client
      .getChannelInfo(token, channelUrl)
      .thenApply(VkDataClient.DataWrapper::data);
  }
}
