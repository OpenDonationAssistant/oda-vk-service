package io.github.opendonationassistant.vk.webhook.handlers;

import static io.github.opendonationassistant.vk.webhook.handlers.HandlerTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.opendonationassistant.rabbit.RabbitClient;
import io.github.opendonationassistant.vk.account.VkAccountRepository;
import io.github.opendonationassistant.vk.events.VKLiveStreamRecordEvent;
import io.github.opendonationassistant.vk.metrics.VkMetrics;
import io.github.opendonationassistant.vk.webhook.VkEventType;
import io.github.opendonationassistant.vk.webhook.VkWebhook;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class StreamRecordCreateHandlerTest {

  VkAccountRepository accountRepository = mock(VkAccountRepository.class);
  RabbitClient events = mock(RabbitClient.class);
  VkMetrics metrics = new VkMetrics(new SimpleMeterRegistry());
  StreamRecordCreateHandler handler = new StreamRecordCreateHandler(
    new AccountEventForwarder(accountRepository, events, metrics)
  );

  @Test
  public void testTypeIsStreamRecordNew() {
    assertEquals(VkEventType.CHANNEL_STREAM_RECORD_NEW, handler.type());
  }

  @Test
  public void testSendingStreamRecordEvent() throws Exception {
    var account = account("123123123", "recipient-123");
    when(accountRepository.findByVKId("123123123"))
      .thenReturn(List.of(account));

    assertNull(handler.handle(streamRecordEvent()).join());

    var captor = ArgumentCaptor.forClass(VKLiveStreamRecordEvent.class);

    verify(events).sendEvent(captor.capture());

    var sent = captor.getValue();
    assertEquals("7029219", sent.id());
    assertEquals("recipient-123", sent.recipientId());
    assertEquals("16068645898020", sent.videoId());
    assertEquals(
      "https://images.live.vkvideo.ru/public_video_stream/record/6a1f57e6-bb2a-472a-9310-0d65988e1c66/custom_preview?change_time=1785599324",
      sent.previewUrl()
    );
    assertEquals(
      "Мама, я поехал | The Mound: Omen of Cthulhu ",
      sent.title()
    );
    assertEquals(Instant.ofEpochSecond(1785577998L), sent.startedAt());
    assertEquals(20768L, sent.duration());
  }

  @Test
  public void testNotSendingWithoutAccount() throws Exception {
    when(accountRepository.findByVKId("123123123")).thenReturn(List.of());

    assertNull(handler.handle(streamRecordEvent()).join());

    verify(events, never()).sendEvent(any());
  }

  @Test
  public void testNotSendingWithoutStreamRecord() throws Exception {
    var account = account("123123123", "recipient-123");
    when(accountRepository.findByVKId("123123123"))
      .thenReturn(List.of(account));

    var event = new VkWebhook.Event(
      "7029219",
      "channel_stream_record_new",
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
    assertNull(handler.handle(eventWith(streamRecord(null))).join());
    verify(events, never()).sendEvent(any());
  }

  private void stubAccount() {
    var account = account("123123123", "recipient-123");
    when(accountRepository.findByVKId("123123123"))
      .thenReturn(List.of(account));
  }

  private VkWebhook.Event eventWith(
    VkWebhook.@Nullable StreamRecord streamRecord
  ) {
    return new VkWebhook.Event(
      "7029219",
      "channel_stream_record_new",
      "123123123",
      new VkWebhook.EventData(
        demand(),
        follow(),
        streamRecord,
        null,
        null,
        null,
        new VkWebhook.Channel(12312311L, "someuser"),
        new VkWebhook.Owner(123123123L)
      )
    );
  }

  private VkWebhook.StreamRecord streamRecord(@Nullable Long streamStartedAt) {
    return new VkWebhook.StreamRecord(
      "6a1f57e6-bb2a-472a-9310-0d65988e1c66",
      streamStartedAt,
      20768L,
      "done",
      category(),
      "stream title",
      "16068645898020",
      List.of(),
      1785598898L,
      "https://preview"
    );
  }

  private VkWebhook.Event streamRecordEvent() {
    var category = new VkWebhook.Category(
      "game",
      "The Mound: Omen of Cthulhu",
      "06cd6178-35c2-4998-b6b7-924611595c0c"
    );
    var sourceUrls = List.of(
      new VkWebhook.SourceUrl("full_hd", "https://vd375.okcdn.ru/full_hd"),
      new VkWebhook.SourceUrl("ondemand_dash", "https://vd375.okcdn.ru/dash")
    );
    var streamRecord = new VkWebhook.StreamRecord(
      "6a1f57e6-bb2a-472a-9310-0d65988e1c66",
      1785577998L,
      20768L,
      "done",
      category,
      "Мама, я поехал | The Mound: Omen of Cthulhu ",
      "16068645898020",
      sourceUrls,
      1785598898L,
      "https://images.live.vkvideo.ru/public_video_stream/record/6a1f57e6-bb2a-472a-9310-0d65988e1c66/custom_preview?change_time=1785599324"
    );
    var data = new VkWebhook.EventData(
      demand(),
      follow(),
      streamRecord,
      null,
      null,
      null,
      new VkWebhook.Channel(12312311L, "someuser"),
      new VkWebhook.Owner(123123123L)
    );
    return new VkWebhook.Event(
      "7029219",
      "channel_stream_record_new",
      "123123123",
      data
    );
  }

}
