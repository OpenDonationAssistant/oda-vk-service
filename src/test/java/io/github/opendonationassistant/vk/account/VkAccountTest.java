package io.github.opendonationassistant.vk.account;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.opendonationassistant.integration.MediaRPC;
import io.github.opendonationassistant.integration.VkClient;
import io.github.opendonationassistant.integration.VkClientFactory;
import io.github.opendonationassistant.vk.reward.RewardData;
import io.github.opendonationassistant.vk.reward.RewardDataRepository;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

public class VkAccountTest {

  private static final String REWARD_ID =
    "39a919a7-fd10-4ece-9606-dde43b595fc9";
  private static final String RECIPIENT_ID = "recipient-123";
  private static final String REFRESH_TOKEN_ID = "refresh-token-id";
  private static final String CHANNEL_URL = "stcarolas";

  private final VkClient vkClient = mock(VkClient.class);
  private final VkClientFactory vkClientFactory = mock(VkClientFactory.class);
  private final RewardDataRepository rewardRepository = mock(
    RewardDataRepository.class
  );
  private final MediaRPC media = mock(MediaRPC.class);

  private final VkAccount account = new VkAccount(
    new VkAccountData(
      "id",
      "29586153",
      "stCarolas",
      CHANNEL_URL,
      RECIPIENT_ID,
      REFRESH_TOKEN_ID
    ),
    vkClientFactory,
    rewardRepository,
    media
  );

  @Test
  public void testFindsRewardForDemand() {
    givenKnownReward();

    var found = account.reward(REWARD_ID).join();

    assertTrue(found.isPresent());
    assertEquals(REWARD_ID, found.get().data().id());
    // TODO test all fields from found
    verify(rewardRepository).findOneById(REWARD_ID);
  }

  @Test
  public void testReturnsEmptyForUnknownReward() {
    when(rewardRepository.findOneById(REWARD_ID)).thenReturn(
      CompletableFuture.completedFuture(Optional.empty())
    );

    assertTrue(account.reward(REWARD_ID).join().isEmpty());
    verifyNoInteractions(media, vkClient);
  }

  private void givenKnownReward() {
    when(rewardRepository.findOneById(REWARD_ID)).thenReturn(
      CompletableFuture.completedFuture(
        Optional.of(new RewardData(REWARD_ID, "id", "widget-id", "media"))
      )
    );
  }
}
