package io.github.opendonationassistant.vk.webhook.handlers;

import static io.github.opendonationassistant.vk.webhook.handlers.HandlerTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.opendonationassistant.rabbit.RabbitClient;
import io.github.opendonationassistant.vk.account.VkAccountRepository;
import io.github.opendonationassistant.vk.events.VKLiveChannelSubscriptionEvent;
import io.github.opendonationassistant.vk.metrics.VkMetrics;
import io.github.opendonationassistant.vk.webhook.VkEventType;
import io.github.opendonationassistant.vk.webhook.VkWebhook;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class SubscriptionRenewHandlerTest {

  VkAccountRepository accountRepository = mock(VkAccountRepository.class);
  RabbitClient events = mock(RabbitClient.class);
  VkMetrics metrics = new VkMetrics(new SimpleMeterRegistry());
  SubscriptionRenewHandler handler = new SubscriptionRenewHandler(
    new AccountEventForwarder(accountRepository, events, metrics)
  );

  @Test
  public void testTypeIsSubscriptionRenew() {
    assertEquals(VkEventType.CHANNEL_SUBSCRIPTION_RENEW, handler.type());
  }

  @Test
  public void testSendingSubscriptionEvent() throws Exception {
    var account = account("123123123", "recipient-123");
    when(accountRepository.findByVKId("123123123"))
      .thenReturn(List.of(account));

    assertNull(handler.handle(subscriptionRenewEvent()).join());

    var captor = ArgumentCaptor.forClass(VKLiveChannelSubscriptionEvent.class);

    verify(events).sendEvent(captor.capture());

    var sent = captor.getValue();
    assertEquals("6889830", sent.id());
    assertEquals("recipient-123", sent.recipientId());
    assertEquals("nata_tam", sent.username());
    assertEquals("Часть комьюнити", sent.tier());
    assertEquals(100L, sent.amount());
    assertEquals(Instant.ofEpochSecond(1777073477L), sent.renewedAt());
  }

  @Test
  public void testNotSendingWithoutAccount() throws Exception {
    when(accountRepository.findByVKId("123123123")).thenReturn(List.of());

    assertNull(handler.handle(subscriptionRenewEvent()).join());

    verify(events, never()).sendEvent(any());
  }

  @Test
  public void testNotSendingWithoutSubscription() throws Exception {
    var account = account("123123123", "recipient-123");
    when(accountRepository.findByVKId("123123123"))
      .thenReturn(List.of(account));

    var event = new VkWebhook.Event(
      "6889830",
      "channel_subscription_renew",
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
  public void testNotSendingWithoutSubscriber() throws Exception {
    stubAccount();
    assertNull(handler.handle(eventWith(subscription(level(), null))).join());
    verify(events, never()).sendEvent(any());
  }

  @Test
  public void testNotSendingWithoutLevel() throws Exception {
    stubAccount();
    assertNull(
      handler.handle(eventWith(subscription(null, subscriber()))).join()
    );
    verify(events, never()).sendEvent(any());
  }

  @Test
  public void testNotSendingWithoutRenewedAt() throws Exception {
    stubAccount();
    var withoutRenewedAt = new VkWebhook.Subscription(
      level(),
      30L,
      subscriber(),
      100L,
      null,
      1777073477L
    );
    assertNull(handler.handle(eventWith(withoutRenewedAt)).join());
    verify(events, never()).sendEvent(any());
  }

  private void stubAccount() {
    var account = account("123123123", "recipient-123");
    when(accountRepository.findByVKId("123123123"))
      .thenReturn(List.of(account));
  }

  private VkWebhook.Event eventWith(
    VkWebhook.@Nullable Subscription subscription
  ) {
    return new VkWebhook.Event(
      "6889830",
      "channel_subscription_renew",
      "123123123",
      new VkWebhook.EventData(
        demand(),
        follow(),
        null,
        null,
        subscription,
        null,
        new VkWebhook.Channel(12312311L, "someuser"),
        new VkWebhook.Owner(123123123L)
      )
    );
  }

  private VkWebhook.Subscription subscription(
    VkWebhook.@Nullable Level level,
    VkWebhook.@Nullable Subscriber subscriber
  ) {
    return new VkWebhook.Subscription(
      level,
      30L,
      subscriber,
      100L,
      1777073477L,
      1777073477L
    );
  }

  private VkWebhook.Level level() {
    return new VkWebhook.Level("Часть комьюнити", 100L, "5598");
  }

  private VkWebhook.Subscriber subscriber() {
    return new VkWebhook.Subscriber(
      20694169L,
      "nata_tam",
      "https://avatar",
      7
    );
  }

  private VkWebhook.Event subscriptionRenewEvent() {
    var level = new VkWebhook.Level("Часть комьюнити", 100L, "5598");
    var subscriber = new VkWebhook.Subscriber(
      20694169L,
      "nata_tam",
      "https://images.live.vkvideo.ru/user/20694169/avatar?change_time=1745413022",
      7
    );
    var subscription = new VkWebhook.Subscription(
      level,
      30L,
      subscriber,
      100L,
      1777073477L,
      1777073477L
    );
    var data = new VkWebhook.EventData(
      demand(),
      follow(),
      null,
      null,
      subscription,
      null,
      new VkWebhook.Channel(12312311L, "someuser"),
      new VkWebhook.Owner(123123123L)
    );
    return new VkWebhook.Event(
      "6889830",
      "channel_subscription_renew",
      "123123123",
      data
    );
  }

}
