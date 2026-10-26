package io.github.opendonationassistant.vk.webhook.handlers;

import static io.github.opendonationassistant.vk.webhook.handlers.HandlerTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.opendonationassistant.rabbit.RabbitClient;
import io.github.opendonationassistant.vk.account.VkAccountRepository;
import io.github.opendonationassistant.vk.events.VKLiveChannelFollowEvent;
import io.github.opendonationassistant.vk.metrics.VkMetrics;
import io.github.opendonationassistant.vk.webhook.VkEventType;
import io.github.opendonationassistant.vk.webhook.VkWebhook;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ChannelFollowCreateHandlerTest {

  VkAccountRepository accountRepository = mock(VkAccountRepository.class);
  RabbitClient events = mock(RabbitClient.class);
  SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  VkMetrics metrics = new VkMetrics(meterRegistry);
  ChannelFollowCreateHandler handler = new ChannelFollowCreateHandler(
    new AccountEventForwarder(accountRepository, events, metrics)
  );

  @Test
  public void testTypeIsChannelFollowCreate() {
    assertEquals(VkEventType.CHANNEL_FOLLOW_CREATE, handler.type());
  }

  @Test
  public void testSendingFollowEvent() throws Exception {
    var account = account("123123123", "recipient-123");
    when(accountRepository.findByVKId("123123123"))
      .thenReturn(List.of(account));

    assertNull(handler.handle(followEvent()).join());

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

    assertNull(handler.handle(followEvent()).join());

    verify(events, never()).sendEvent(any());
    assertEquals(
      1,
      meterRegistry
        .counter(
          VkMetrics.EVENTS_NOACCOUNT_METRIC_NAME,
          VkMetrics.TYPE_TAG,
          "channel_follow_create"
        )
        .count()
    );
  }

  @Test
  public void testNotSendingWithoutFollow() throws Exception {
    stubAccount();
    assertNull(handler.handle(eventWith(null)).join());
    verify(events, never()).sendEvent(any());
  }

  @Test
  public void testNotSendingWithoutFollower() throws Exception {
    stubAccount();
    assertNull(
      handler.handle(eventWith(new VkWebhook.Follow(1785489039L, null))).join()
    );
    verify(events, never()).sendEvent(any());
  }

  @Test
  public void testNotSendingWithoutFollowStartedAt() throws Exception {
    stubAccount();
    assertNull(
      handler.handle(eventWith(new VkWebhook.Follow(null, follower()))).join()
    );
    verify(events, never()).sendEvent(any());
  }

  private void stubAccount() {
    var account = account("123123123", "recipient-123");
    when(accountRepository.findByVKId("123123123"))
      .thenReturn(List.of(account));
  }

  private VkWebhook.Event eventWith(VkWebhook.@Nullable Follow follow) {
    return new VkWebhook.Event(
      "6987495",
      "channel_follow_create",
      "123123123",
      new VkWebhook.EventData(
        demand(),
        follow,
        null,
        null,
        null,
        null,
        new VkWebhook.Channel(12312311L, "someuser"),
        new VkWebhook.Owner(123123123L)
      )
    );
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
