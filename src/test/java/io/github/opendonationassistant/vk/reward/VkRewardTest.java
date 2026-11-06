package io.github.opendonationassistant.vk.reward;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.github.opendonationassistant.integration.MediaRPC;
import io.github.opendonationassistant.integration.VkClient;
import io.github.opendonationassistant.integration.VkClientFactory;
import io.github.opendonationassistant.integration.VkDataClient;
import io.github.opendonationassistant.vk.account.VkAccount;
import io.github.opendonationassistant.vk.account.VkAccountData;
import io.github.opendonationassistant.vk.webhook.VkWebhook;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class VkRewardTest {

  private static final String REWARD_ID =
    "39a919a7-fd10-4ece-9606-dde43b595fc9";
  private static final String RECIPIENT_ID = "recipient-123";
  private static final String REFRESH_TOKEN_ID = "refresh-token-id";
  private static final String CHANNEL_URL = "stcarolas";
  private static final Long DEMAND_ID = 8433849L;
  private static final Long REWARDER_VK_ID = 15346984L;

  private static final VkAccountData ACCOUNT_DATA = new VkAccountData(
    "id",
    "29586153",
    "stCarolas",
    CHANNEL_URL,
    RECIPIENT_ID,
    REFRESH_TOKEN_ID
  );

  /**
   * UUIDv3("vk:recipient-123:29586153:demand:8433849"). Pinned so that a
   * silent change to the idempotency seed is caught: the media service uses
   * this value as its primary key, so a changed seed would duplicate existing
   * media.
   */
  private static final String EXPECTED_MEDIA_ID =
    "de31e00e-b25c-30e8-a90b-ad94b05c9de4";

  private final VkClient vkClient = mock(VkClient.class);
  private final VkClientFactory vkClientFactory = mock(VkClientFactory.class);
  private final MediaRPC media = mock(MediaRPC.class);
  private final VkReward reward = rewardFor(ACCOUNT_DATA);

  @BeforeEach
  public void setUp() {
    when(vkClientFactory.create(anyString(), anyString())).thenReturn(vkClient);
    when(
      vkClient.acceptReward(
        anyString(),
        any(VkDataClient.DemandsRequest.class)
      )
    ).thenReturn(CompletableFuture.completedFuture(null));
    when(
      vkClient.rejectReward(
        anyString(),
        any(VkDataClient.DemandsRequest.class)
      )
    ).thenReturn(CompletableFuture.completedFuture(null));
    when(
      vkClient.sendChatMessage(
        anyString(),
        any(VkDataClient.ChatMessageRequest.class)
      )
    ).thenReturn(CompletableFuture.completedFuture(null));
    when(media.add(any(MediaRPC.AddMediaRequest.class))).thenReturn(
      new MediaRPC.AddMediaResponse("media-id", "")
    );
  }

  @Test
  public void testAddsMediaAndAcceptsReward() {
    assertNull(reward.accept(demand()).join());

    var mediaCaptor = ArgumentCaptor.forClass(MediaRPC.AddMediaRequest.class);
    verify(media).add(mediaCaptor.capture());

    var mediaRequest = mediaCaptor.getValue();
    assertEquals(EXPECTED_MEDIA_ID, mediaRequest.id());
    assertEquals("https://ya.ru", mediaRequest.url());
    assertEquals("stCarolas", mediaRequest.requester());
    assertEquals(RECIPIENT_ID, mediaRequest.recipientId());
    assertEquals(VkAccount.VK_LIVE_SYSTEM, mediaRequest.system());

    var acceptCaptor = ArgumentCaptor.forClass(
      VkDataClient.DemandsRequest.class
    );
    verify(vkClientFactory).create(RECIPIENT_ID, REFRESH_TOKEN_ID);
    verify(vkClient).acceptReward(eq(CHANNEL_URL), acceptCaptor.capture());
    assertEquals(
      List.of(new VkDataClient.Demand(DEMAND_ID)),
      acceptCaptor.getValue().demands()
    );

    verify(vkClient, never()).rejectReward(
      anyString(),
      any(VkDataClient.DemandsRequest.class)
    );
    verify(vkClient, never()).sendChatMessage(
      anyString(),
      any(VkDataClient.ChatMessageRequest.class)
    );
  }

  @Test
  public void testDerivesSameMediaIdForRedeliveredEvent() {
    assertNull(reward.accept(demand()).join());
    assertNull(reward.accept(demand()).join());

    var mediaCaptor = ArgumentCaptor.forClass(MediaRPC.AddMediaRequest.class);
    verify(media, times(2)).add(mediaCaptor.capture());
    assertEquals(
      List.of(EXPECTED_MEDIA_ID, EXPECTED_MEDIA_ID),
      mediaCaptor
        .getAllValues()
        .stream()
        .map(MediaRPC.AddMediaRequest::id)
        .toList()
    );
  }

  @Test
  public void testMediaIdIsUuidVersion3() {
    assertNull(reward.accept(demand()).join());

    var mediaCaptor = ArgumentCaptor.forClass(MediaRPC.AddMediaRequest.class);
    verify(media).add(mediaCaptor.capture());
    assertEquals(3, UUID.fromString(mediaCaptor.getValue().id()).version());
  }

  @Test
  public void testSkipsMediaAndRewardCallsWhenDemandIdIsMissing() {
    assertNull(reward.accept(demand(null, REWARDER_VK_ID)).join());

    verifyNoInteractions(media);
    verify(vkClient, never()).acceptReward(
      anyString(),
      any(VkDataClient.DemandsRequest.class)
    );
    verify(vkClient, never()).rejectReward(
      anyString(),
      any(VkDataClient.DemandsRequest.class)
    );
    assertTrue(
      sentChatMessageText().contains(
        "не удалось определить идентификатор награды"
      )
    );
  }

  @Test
  public void testRejectsRewardAndNotifiesChatWhenMediaRpcFails() {
    when(media.add(any(MediaRPC.AddMediaRequest.class))).thenThrow(
      new RuntimeException("rpc timeout")
    );

    assertNull(reward.accept(demand()).join());

    verify(vkClient, never()).acceptReward(
      anyString(),
      any(VkDataClient.DemandsRequest.class)
    );
    assertRejectedAndReported("rpc timeout");
  }

  @Test
  public void testRejectsRewardAndNotifiesChatWhenMediaIsRejected() {
    when(media.add(any(MediaRPC.AddMediaRequest.class))).thenReturn(
      new MediaRPC.AddMediaResponse(null, "Видео не найдено")
    );

    assertNull(reward.accept(demand()).join());

    verify(vkClient, never()).acceptReward(
      anyString(),
      any(VkDataClient.DemandsRequest.class)
    );
    assertRejectedAndReported("Видео не найдено");
  }

  @Test
  public void testRejectsRewardAndNotifiesChatWhenMediaResponseIsNull() {
    doReturn(null).when(media).add(any(MediaRPC.AddMediaRequest.class));

    assertNull(reward.accept(demand()).join());

    verify(vkClient, never()).acceptReward(
      anyString(),
      any(VkDataClient.DemandsRequest.class)
    );
    assertRejectedAndReported("нет ответа от сервиса медиа");
  }

  @Test
  public void testFallsBackToNickWhenRewarderIdIsMissing() {
    when(media.add(any(MediaRPC.AddMediaRequest.class))).thenThrow(
      new RuntimeException("rpc timeout")
    );

    assertNull(reward.accept(demand((Long) null)).join());

    var parts = sentChatMessage().parts();
    assertEquals(1, parts.size());
    assertNull(parts.get(0).mention());
    assertTrue(
      Objects.requireNonNull(parts.get(0).text())
        .content()
        .startsWith("@stCarolas, ")
    );
  }

  @Test
  public void testStillRejectsRewardWhenChatNotificationFails() {
    when(media.add(any(MediaRPC.AddMediaRequest.class))).thenThrow(
      new RuntimeException("rpc timeout")
    );
    when(
      vkClient.sendChatMessage(
        anyString(),
        any(VkDataClient.ChatMessageRequest.class)
      )
    ).thenReturn(
      CompletableFuture.failedFuture(new RuntimeException("chat down"))
    );

    assertNull(reward.accept(demand()).join());

    verify(vkClient).rejectReward(
      eq(CHANNEL_URL),
      any(VkDataClient.DemandsRequest.class)
    );
  }

  @Test
  public void testStillNotifiesChatWhenRejectFails() {
    when(media.add(any(MediaRPC.AddMediaRequest.class))).thenThrow(
      new RuntimeException("rpc timeout")
    );
    when(
      vkClient.rejectReward(
        anyString(),
        any(VkDataClient.DemandsRequest.class)
      )
    ).thenReturn(
      CompletableFuture.failedFuture(new RuntimeException("reject down"))
    );

    assertNull(reward.accept(demand()).join());

    var text = sentChatMessageText();
    assertTrue(text.contains("Не удалось автоматически вернуть баллы"));
  }

  @Test
  public void testStillNotifiesChatWhenRejectThrowsSynchronously() {
    when(media.add(any(MediaRPC.AddMediaRequest.class))).thenThrow(
      new RuntimeException("rpc timeout")
    );
    when(
      vkClient.rejectReward(
        anyString(),
        any(VkDataClient.DemandsRequest.class)
      )
    ).thenThrow(new RuntimeException("token rpc down"));

    assertNull(reward.accept(demand()).join());

    var text = sentChatMessageText();
    assertTrue(text.contains("Не удалось автоматически вернуть баллы"));
  }

  @Test
  public void testStillRejectsRewardWhenChatThrowsSynchronously() {
    when(media.add(any(MediaRPC.AddMediaRequest.class))).thenThrow(
      new RuntimeException("rpc timeout")
    );
    when(
      vkClient.sendChatMessage(
        anyString(),
        any(VkDataClient.ChatMessageRequest.class)
      )
    ).thenThrow(new RuntimeException("token rpc down"));

    assertNull(reward.accept(demand()).join());

    verify(vkClient).rejectReward(
      eq(CHANNEL_URL),
      any(VkDataClient.DemandsRequest.class)
    );
  }

  private VkReward rewardFor(VkAccount account) {
    return new VkReward(
      new RewardData(REWARD_ID, "id", "widget-id", "media"),
      account,
      vkClientFactory,
      media
    );
  }

  private VkReward rewardFor(VkAccountData accountData) {
    var account = mock(VkAccount.class);
    when(account.data()).thenReturn(accountData);
    return rewardFor(account);
  }

  private void assertRejectedAndReported(String reason) {
    var rejectCaptor = ArgumentCaptor.forClass(
      VkDataClient.DemandsRequest.class
    );
    verify(vkClient).rejectReward(eq(CHANNEL_URL), rejectCaptor.capture());
    assertEquals(
      List.of(new VkDataClient.Demand(DEMAND_ID)),
      rejectCaptor.getValue().demands()
    );

    var parts = sentChatMessage().parts();
    assertEquals(2, parts.size());
    assertEquals(
      new VkDataClient.Mention(REWARDER_VK_ID),
      parts.get(0).mention()
    );
    var text = sentChatMessageText();
    assertTrue(
      text.contains(reason),
      "message should contain the failure reason"
    );
    assertTrue(text.contains("Баллы возвращены"));
  }

  private String sentChatMessageText() {
    var parts = sentChatMessage().parts();
    assertEquals(2, parts.size());
    return Objects.requireNonNull(parts.get(1).text()).content();
  }

  private VkDataClient.ChatMessageRequest sentChatMessage() {
    var messageCaptor = ArgumentCaptor.forClass(
      VkDataClient.ChatMessageRequest.class
    );
    verify(vkClient).sendChatMessage(
      eq(CHANNEL_URL),
      messageCaptor.capture()
    );
    return messageCaptor.getValue();
  }

  private VkWebhook.Demand demand() {
    return demand(DEMAND_ID, REWARDER_VK_ID);
  }

  private VkWebhook.Demand demand(@Nullable Long rewarderVkId) {
    return demand(DEMAND_ID, rewarderVkId);
  }

  @SuppressWarnings("NullAway")
  private VkWebhook.Demand demand(
    @Nullable Long demandId,
    @Nullable Long rewarderVkId
  ) {
    return new VkWebhook.Demand(
      demandId,
      new VkWebhook.User("stCarolas", rewarderVkId),
      new VkWebhook.Reward(REWARD_ID),
      "pending",
      List.of(Map.of("link", new VkWebhook.Part("https://ya.ru")))
    );
  }
}
