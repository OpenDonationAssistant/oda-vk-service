package io.github.opendonationassistant.vk.webhook.handlers;

import static io.github.opendonationassistant.vk.webhook.handlers.HandlerTestFixtures.account;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.rabbit.RabbitClient;
import io.github.opendonationassistant.vk.account.VkAccountRepository;
import io.github.opendonationassistant.vk.events.VKLiveChannelFollowEvent;
import io.github.opendonationassistant.vk.metrics.VkMetrics;
import io.github.opendonationassistant.vk.webhook.VkWebhook;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class AccountEventForwarderTest {

  VkAccountRepository accountRepository = mock(VkAccountRepository.class);
  RabbitClient events = mock(RabbitClient.class);
  SimpleMeterRegistry registry = new SimpleMeterRegistry();
  VkMetrics metrics = new VkMetrics(registry);
  AccountEventForwarder forwarder = new AccountEventForwarder(
    accountRepository,
    events,
    metrics
  );

  @Test
  public void publishesEventForResolvedAccount() throws Exception {
    var account = account("123123123", "recipient-123");
    when(accountRepository.findByVKId("123123123"))
      .thenReturn(List.of(account));

    var result = forwarder.forward(event(), factory());

    assertNull(result.join());
    var captor = ArgumentCaptor.forClass(VKLiveChannelFollowEvent.class);
    verify(events).sendEvent(captor.capture());
    assertEquals("recipient-123", captor.getValue().recipientId());
  }

  @Test
  public void doesNotPublishWithoutAccountAndCountsMetric() throws Exception {
    when(accountRepository.findByVKId("123123123")).thenReturn(List.of());

    var result = forwarder.forward(event(), factory());

    assertNull(result.join());
    verify(events, never()).sendEvent(any());
    assertEquals(
      1,
      registry
        .counter(
          VkMetrics.EVENTS_NOACCOUNT_METRIC_NAME,
          VkMetrics.TYPE_TAG,
          "channel_follow_create"
        )
        .count()
    );
  }

  @Test
  public void skipsAccountLookupWhenFactoryIsNull() throws Exception {
    var result = forwarder.forward(event(), null);

    assertNull(result.join());
    verifyNoInteractions(accountRepository);
    verify(events, never()).sendEvent(any());
  }

  @Test
  public void completesExceptionallyWhenPublishingFails() throws Exception {
    var account = account("123123123", "recipient-123");
    when(accountRepository.findByVKId("123123123"))
      .thenReturn(List.of(account));
    doThrow(new IOException("broker down")).when(events).sendEvent(any());

    var result = forwarder.forward(event(), factory());

    assertTrue(result.isCompletedExceptionally());
  }

  @Test
  public void completesExceptionallyWhenPublishingFailsWithRuntimeException()
    throws Exception {
    var account = account("123123123", "recipient-123");
    when(accountRepository.findByVKId("123123123"))
      .thenReturn(List.of(account));
    doThrow(new RuntimeException("broker down")).when(events).sendEvent(any());

    var result = forwarder.forward(event(), factory());

    assertTrue(result.isCompletedExceptionally());
  }

  private AccountEventFactory factory() {
    return recipientId ->
      new VKLiveChannelFollowEvent(
        "event-id",
        recipientId,
        "follower",
        Instant.ofEpochSecond(1L)
      );
  }

  private VkWebhook.Event event() {
    return new VkWebhook.Event(
      "6987495",
      "channel_follow_create",
      "123123123",
      new VkWebhook.EventData(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
      )
    );
  }
}
