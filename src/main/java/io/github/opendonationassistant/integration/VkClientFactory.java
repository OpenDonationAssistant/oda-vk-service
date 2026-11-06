package io.github.opendonationassistant.integration;

import io.github.opendonationassistant.rabbit.TokenRPC;
import io.github.opendonationassistant.rabbit.TokenRPC.TokenRequest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class VkClientFactory {

  private static final String BEARER_PREFIX = "Bearer ";

  private final VkDataClient client;
  private final TokenRPC tokenRPC;

  @Inject
  public VkClientFactory(VkDataClient client, TokenRPC tokenRPC) {
    this.client = client;
    this.tokenRPC = tokenRPC;
  }

  public VkClient create(String recipientId, String refreshTokenId) {
    var response = tokenRPC.token(
      new TokenRequest(recipientId, refreshTokenId)
    );
    if (
      response == null || response.token() == null || response.token().isBlank()
    ) {
      throw new IllegalStateException(
        "No VK access token for recipient %s".formatted(recipientId)
      );
    }
    return new VkClient(client, BEARER_PREFIX + response.token());
  }
}
