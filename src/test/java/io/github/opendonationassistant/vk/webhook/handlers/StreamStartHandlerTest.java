package io.github.opendonationassistant.vk.webhook.handlers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.opendonationassistant.rabbit.RabbitClient;
import io.github.opendonationassistant.vk.account.VkAccount;
import io.github.opendonationassistant.vk.account.VkAccountData;
import io.github.opendonationassistant.vk.account.VkAccountRepository;
import io.github.opendonationassistant.vk.events.VKLiveStreamStartedEvent;
import io.github.opendonationassistant.vk.webhook.VkWebhook;
import java.time.Instant;
import java.util.List;

import org.instancio.junit.Given;
import org.instancio.junit.InstancioExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(InstancioExtension.class)
public class StreamStartHandlerTest {

  VkAccountRepository accountRepository = mock(VkAccountRepository.class);
  RabbitClient events = mock(RabbitClient.class);
  StreamStartHandler handler = new StreamStartHandler(accountRepository, events);

  @Test
  public void testCanHandleStreamStart(@Given String randomType) {
    assertTrue(handler.canHandle("channel_stream_start"));
    assertFalse(handler.canHandle(randomType));
  }

  @Test
  public void testSendingStreamStartedEvent() throws Exception {
    var account = account("123123123", "recipient-123");
    when(accountRepository.findByVKId("123123123"))
      .thenReturn(List.of(account));

    handler.handle(streamStartEvent());

    var captor = ArgumentCaptor.forClass(VKLiveStreamStartedEvent.class);

    verify(events).sendEvent(captor.capture());

    var sent = captor.getValue();
    assertEquals("7020679", sent.id());
    assertEquals("recipient-123", sent.recipientId());
    assertEquals("16068645898020", sent.videoId());
    assertEquals(
      "https://images.live.vkvideo.ru/image/bda0b6c2-68e8-4179-af49-e53d0799b8d3?change_time=1781502910",
      sent.previewUrl()
    );
    assertEquals(
      "Мама, я поехал | The Mound: Omen of Cthulhu ",
      sent.title()
    );
    assertEquals(Instant.ofEpochSecond(1785577998L), sent.startedAt());
  }

  @Test
  public void testNotSendingWithoutAccount() throws Exception {
    when(accountRepository.findByVKId("123123123")).thenReturn(List.of());

    handler.handle(streamStartEvent());

    verify(events, never()).sendEvent(any());
  }

  @Test
  public void testNotSendingWithoutStream() throws Exception {
    var account = account("123123123", "recipient-123");
    when(accountRepository.findByVKId("123123123"))
      .thenReturn(List.of(account));

    var event = new VkWebhook.Event(
      "7020679",
      "channel_stream_start",
      "123123123",
      new VkWebhook.EventData(
        demand(),
        follow(),
        null,
        null,
        null,
        new VkWebhook.Channel(12312311L, "someuser"),
        new VkWebhook.Owner(123123123L)
      )
    );

    handler.handle(event);

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

  private VkWebhook.Event streamStartEvent() {
    var category = new VkWebhook.Category(
      "game",
      "The Mound: Omen of Cthulhu",
      "06cd6178-35c2-4998-b6b7-924611595c0c"
    );
    var stream = new VkWebhook.Stream(
      "6a1f57e6-bb2a-472a-9310-0d65988e1c66",
      "https://images.live.vkvideo.ru/image/bda0b6c2-68e8-4179-af49-e53d0799b8d3?change_time=1781502910",
      null,
      1785577998L,
      "Мама, я поехал | The Mound: Omen of Cthulhu ",
      "16068645898020",
      category
    );
    var data = new VkWebhook.EventData(
      demand(),
      follow(),
      null,
      stream,
      null,
      new VkWebhook.Channel(12312311L, "someuser"),
      new VkWebhook.Owner(123123123L)
    );
    return new VkWebhook.Event(
      "7020679",
      "channel_stream_start",
      "123123123",
      data
    );
  }

  private VkWebhook.Demand demand() {
    return new VkWebhook.Demand(
      8433849L,
      new VkWebhook.User("stCarolas"),
      new VkWebhook.Reward("39a919a7-fd10-4ece-9606-dde43b595fc9"),
      "pending",
      List.of()
    );
  }

  private VkWebhook.Follow follow() {
    return new VkWebhook.Follow(
      1785489039L,
      new VkWebhook.Follower(15, 19219592L, "OneTuz", "https://avatar")
    );
  }
}
