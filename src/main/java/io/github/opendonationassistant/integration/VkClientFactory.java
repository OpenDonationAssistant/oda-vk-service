package io.github.opendonationassistant.integration;

import io.github.opendonationassistant.rabbit.AsyncTokenRPC;
import io.github.opendonationassistant.rabbit.TokenRPC.TokenRequest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.CompletableFuture;

@Singleton
public class VkClientFactory {

  private static final String BEARER_PREFIX = "Bearer ";

  private final VkDataClient client;
  private final AsyncTokenRPC asyncTokenRpc;

  @Inject
  public VkClientFactory(VkDataClient client, AsyncTokenRPC asyncTokenRpc) {
    this.client = client;
    this.asyncTokenRpc = asyncTokenRpc;
  }

  public CompletableFuture<VkClient> create(
    String recipientId,
    String refreshTokenId
  ) {
    return asyncTokenRpc
      .token(new TokenRequest(recipientId, refreshTokenId))
      .thenApply(response -> {
        if (
          response == null ||
          response.token() == null ||
          response.token().isBlank()
        ) {
          throw new IllegalStateException(
            "No VK access token for recipient %s".formatted(recipientId)
          );
        }
        return new VkClient(client, BEARER_PREFIX + response.token());
      });
  }
}
