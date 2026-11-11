package io.github.opendonationassistant.integration;

import io.github.opendonationassistant.rabbit.TokenRPC;
import io.github.opendonationassistant.rabbit.TokenRPC.TokenRequest;
import io.micronaut.scheduling.TaskExecutors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Singleton
public class VkClientFactory {

  private static final String BEARER_PREFIX = "Bearer ";

  private final VkDataClient client;
  private final TokenRPC tokenRPC;
  private final ExecutorService blockingExecutor;

  @Inject
  public VkClientFactory(
    VkDataClient client,
    TokenRPC tokenRPC,
    @Named(TaskExecutors.BLOCKING) ExecutorService blockingExecutor
  ) {
    this.client = client;
    this.tokenRPC = tokenRPC;
    this.blockingExecutor = blockingExecutor;
  }

  public CompletableFuture<VkClient> create(
    String recipientId,
    String refreshTokenId
  ) {
    return CompletableFuture.supplyAsync(
      () -> {
        var response = tokenRPC.token(
          new TokenRequest(recipientId, refreshTokenId)
        );
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
      },
      blockingExecutor
    );
  }
}
