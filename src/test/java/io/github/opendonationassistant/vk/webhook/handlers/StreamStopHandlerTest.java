package io.github.opendonationassistant.vk.webhook.handlers;

import static io.github.opendonationassistant.vk.webhook.handlers.HandlerTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.opendonationassistant.rabbit.RabbitClient;
import io.github.opendonationassistant.vk.account.VkAccountRepository;
import io.github.opendonationassistant.vk.events.VKLiveStreamEndedEvent;
import io.github.opendonationassistant.vk.metrics.VkMetrics;
import io.github.opendonationassistant.vk.webhook.VkEventType;
import io.github.opendonationassistant.vk.webhook.VkWebhook;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class StreamStopHandlerTest {

  VkAccountRepository accountRepository = mock(VkAccountRepository.class);
  RabbitClient events = mock(RabbitClient.class);
  VkMetrics metrics = new VkMetrics(new SimpleMeterRegistry());
  StreamStopHandler handler = new StreamStopHandler(
    new AccountEventForwarder(accountRepository, events, metrics)
  );

  @Test
  public void testTypeIsStreamStop() {
    assertEquals(VkEventType.CHANNEL_STREAM_STOP, handler.type());
  }

  @Test
  public void testSendingStreamEndedEvent() throws Exception {
    var account = account("123123123", "recipient-123");
    when(accountRepository.findByVKId("123123123"))
      .thenReturn(List.of(account));

    assertNull(handler.handle(streamStopEvent()).join());

    var captor = ArgumentCaptor.forClass(VKLiveStreamEndedEvent.class);

    verify(events).sendEvent(captor.capture());

    var sent = captor.getValue();
    assertEquals("7029042", sent.id());
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
    assertEquals(Instant.ofEpochSecond(1785598901L), sent.endedAt());
  }

  @Test
  public void testNotSendingWithoutAccount() throws Exception {
    when(accountRepository.findByVKId("123123123")).thenReturn(List.of());

    assertNull(handler.handle(streamStopEvent()).join());

    verify(events, never()).sendEvent(any());
  }

  @Test
  public void testNotSendingWithoutStream() throws Exception {
    var account = account("123123123", "recipient-123");
    when(accountRepository.findByVKId("123123123"))
      .thenReturn(List.of(account));

    var event = new VkWebhook.Event(
      "7029042",
      "channel_stream_stop",
      "123123123",
      new VkWebhook.EventData(
        demand(),
        follow(),
        null,
        null,
        null,
        null,
        new VkWebhook.Channel(12312311L, "someuser"),
        new VkWebhook.Owner(123123123L)
      )
    );

    assertNull(handler.handle(event).join());

    verify(events, never()).sendEvent(any());
  }

  @Test
  public void testNotSendingWithoutStreamStartedAt() throws Exception {
    stubAccount();
    assertNull(handler.handle(eventWith(stream(null, 1785598901L))).join());
    verify(events, never()).sendEvent(any());
  }

  @Test
  public void testNotSendingWithoutStreamEndedAt() throws Exception {
    stubAccount();
    assertNull(handler.handle(eventWith(stream(1785577998L, null))).join());
    verify(events, never()).sendEvent(any());
  }

  private void stubAccount() {
    var account = account("123123123", "recipient-123");
    when(accountRepository.findByVKId("123123123"))
      .thenReturn(List.of(account));
  }

  private VkWebhook.Event eventWith(VkWebhook.@Nullable Stream stream) {
    return new VkWebhook.Event(
      "7029042",
      "channel_stream_stop",
      "123123123",
      new VkWebhook.EventData(
        demand(),
        follow(),
        null,
        stream,
        null,
        null,
        new VkWebhook.Channel(12312311L, "someuser"),
        new VkWebhook.Owner(123123123L)
      )
    );
  }

  private VkWebhook.Stream stream(
    @Nullable Long startedAt,
    @Nullable Long endedAt
  ) {
    return new VkWebhook.Stream(
      "6a1f57e6-bb2a-472a-9310-0d65988e1c66",
      "https://preview",
      endedAt,
      startedAt,
      "stream title",
      "16068645898020",
      category()
    );
  }

  private VkWebhook.Event streamStopEvent() {
    var category = new VkWebhook.Category(
      "game",
      "The Mound: Omen of Cthulhu",
      "06cd6178-35c2-4998-b6b7-924611595c0c"
    );
    var stream = new VkWebhook.Stream(
      "6a1f57e6-bb2a-472a-9310-0d65988e1c66",
      "https://images.live.vkvideo.ru/image/bda0b6c2-68e8-4179-af49-e53d0799b8d3?change_time=1781502910",
      1785598901L,
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
      null,
      new VkWebhook.Channel(12312311L, "someuser"),
      new VkWebhook.Owner(123123123L)
    );
    return new VkWebhook.Event(
      "7029042",
      "channel_stream_stop",
      "123123123",
      data
    );
  }

}
