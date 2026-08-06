package io.github.opendonationassistant.vk.webhook.handlers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.opendonationassistant.rabbit.RabbitClient;
import io.github.opendonationassistant.vk.account.VkAccount;
import io.github.opendonationassistant.vk.account.VkAccountData;
import io.github.opendonationassistant.vk.account.VkAccountRepository;
import io.github.opendonationassistant.vk.events.VKLiveStreamRecordEvent;
import io.github.opendonationassistant.vk.webhook.VkWebhook;
import java.time.Instant;
import java.util.List;

import org.instancio.junit.Given;
import org.instancio.junit.InstancioExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(InstancioExtension.class)
public class StreamRecordCreateHandlerTest {

  VkAccountRepository accountRepository = mock(VkAccountRepository.class);
  RabbitClient events = mock(RabbitClient.class);
  StreamRecordCreateHandler handler = new StreamRecordCreateHandler(
    accountRepository,
    events
  );

  @Test
  public void testCanHandleStreamRecordNew(@Given String randomType) {
    assertTrue(handler.canHandle("channel_stream_record_new"));
    assertFalse(handler.canHandle(randomType));
  }

  @Test
  public void testSendingStreamRecordEvent() throws Exception {
    var account = account("123123123", "recipient-123");
    when(accountRepository.findByVKId("123123123"))
      .thenReturn(List.of(account));

    handler.handle(streamRecordEvent());

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

    handler.handle(streamRecordEvent());

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
