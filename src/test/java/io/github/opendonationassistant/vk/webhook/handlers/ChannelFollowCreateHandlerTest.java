package io.github.opendonationassistant.vk.webhook.handlers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.opendonationassistant.rabbit.RabbitClient;
import io.github.opendonationassistant.vk.account.VkAccount;
import io.github.opendonationassistant.vk.account.VkAccountData;
import io.github.opendonationassistant.vk.account.VkAccountRepository;
import io.github.opendonationassistant.vk.events.VKLiveChannelFollowEvent;
import io.github.opendonationassistant.vk.webhook.VkWebhook;
import java.time.Instant;
import java.util.List;

import org.instancio.junit.Given;
import org.instancio.junit.InstancioExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(InstancioExtension.class)
public class ChannelFollowCreateHandlerTest {

  VkAccountRepository accountRepository = mock(VkAccountRepository.class);
  RabbitClient events = mock(RabbitClient.class);
  ChannelFollowCreateHandler handler = new ChannelFollowCreateHandler(
    accountRepository,
    events
  );

  @Test
  public void testCanHandleChannelFollowCreate(@Given String randomType) {
    assertTrue(handler.canHandle("channel_follow_create"));
    assertFalse(handler.canHandle(randomType));
  }

  @Test
  public void testSendingFollowEvent() throws Exception {
    var account = account("123123123", "recipient-123");
    when(accountRepository.findByVKId("123123123"))
      .thenReturn(List.of(account));

    handler.handle(followEvent());

    var captor = ArgumentCaptor.forClass(VKLiveChannelFollowEvent.class);

    verify(events).sendEvent(captor.capture());

    var sent = captor.getValue();
    assertEquals("6987495", sent.id());
    assertEquals("recipient-123", sent.recipientId());
    assertEquals("OneTuz", sent.username());
    assertEquals(Instant.ofEpochSecond(1785489039L), sent.timestamp());
  }

  @Test
  public void testNotSendingWithoutAccount() throws Exception {
    when(accountRepository.findByVKId("123123123")).thenReturn(List.of());

    handler.handle(followEvent());

    verify(events, never()).sendEvent(any());
  }

  private VkAccount account(String vkId, String recipientId) {
    var account = mock(VkAccount.class);
    when(account.data())
      .thenReturn(
        new VkAccountData(
          "id",
          vkId,
          "stCarolas",
          "someuser",
          recipientId,
          "refresh-token-id"
        )
      );
    return account;
  }

  private VkWebhook.Event followEvent() {
    var follower = new VkWebhook.Follower(
      15,
      19219592L,
      "OneTuz",
      "https://images.live.vkvideo.ru/user/19219592/avatar"
    );
    var follow = new VkWebhook.Follow(1785489039L, follower);
    var demand = new VkWebhook.Demand(
      8433849L,
      new VkWebhook.User("stCarolas"),
      new VkWebhook.Reward("39a919a7-fd10-4ece-9606-dde43b595fc9"),
      "pending",
      List.of()
    );
    var data = new VkWebhook.EventData(
      demand,
      follow,
      null,
      null,
      null,
            null,
            new VkWebhook.Channel(12312311L, "someuser"),
      new VkWebhook.Owner(123123123L)
    );
    return new VkWebhook.Event(
      "6987495",
      "channel_follow_create",
      "123123123",
      data
    );
  }
}
