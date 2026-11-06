package io.github.opendonationassistant.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.opendonationassistant.rabbit.TokenRPC;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

public class VkClientFactoryTest {

  private static final String RECIPIENT_ID = "recipient-123";
  private static final String REFRESH_TOKEN_ID = "refresh-token-id";
  private static final String CHANNEL_URL = "stcarolas";

  private final VkDataClient data = mock(VkDataClient.class);
  private final TokenRPC tokenRPC = mock(TokenRPC.class);
  private final VkClientFactory factory = new VkClientFactory(data, tokenRPC);

  @Test
  public void testCreatesClientWithResolvedBearerToken() {
    givenToken("access-token");
    var request = demandsRequest();
    when(data.rejectReward("Bearer access-token", CHANNEL_URL, request)).thenReturn(
      CompletableFuture.completedFuture(null)
    );

    factory
      .create(RECIPIENT_ID, REFRESH_TOKEN_ID)
      .rejectReward(CHANNEL_URL, request)
      .join();

    verify(data).rejectReward("Bearer access-token", CHANNEL_URL, request);
  }

  @Test
  public void testFailsWhenTokenResponseIsMissing() {
    when(tokenRPC.token(any(TokenRPC.TokenRequest.class))).thenReturn(null);

    var error = assertThrows(
      IllegalStateException.class,
      () -> factory.create(RECIPIENT_ID, REFRESH_TOKEN_ID)
    );

    assertTrue(
      Objects.requireNonNull(error.getMessage()).contains(RECIPIENT_ID)
    );
  }

  @Test
  @SuppressWarnings("NullAway")
  public void testFailsWhenTokenIsMissing() {
    when(tokenRPC.token(any(TokenRPC.TokenRequest.class))).thenReturn(
      new TokenRPC.TokenResponse(null, "no token")
    );

    assertThrows(
      IllegalStateException.class,
      () -> factory.create(RECIPIENT_ID, REFRESH_TOKEN_ID)
    );
  }

  @Test
  public void testFailsWhenTokenIsBlank() {
    givenToken("  ");

    assertThrows(
      IllegalStateException.class,
      () -> factory.create(RECIPIENT_ID, REFRESH_TOKEN_ID)
    );
  }

  private void givenToken(String token) {
    when(tokenRPC.token(any(TokenRPC.TokenRequest.class))).thenReturn(
      new TokenRPC.TokenResponse(token, "")
    );
  }

  private static VkDataClient.DemandsRequest demandsRequest() {
    return new VkDataClient.DemandsRequest(
      List.of(new VkDataClient.Demand(8433849L))
    );
  }
}
