package io.github.opendonationassistant.vk.webhook.handlers;

import static io.github.opendonationassistant.vk.webhook.handlers.HandlerTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.opendonationassistant.rabbit.RabbitClient;
import io.github.opendonationassistant.vk.account.VkAccountRepository;
import io.github.opendonationassistant.vk.events.VKLiveRaidEvent;
import io.github.opendonationassistant.vk.metrics.VkMetrics;
import io.github.opendonationassistant.vk.webhook.VkEventType;
import io.github.opendonationassistant.vk.webhook.VkWebhook;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class RaidStartHandlerTest {

  VkAccountRepository accountRepository = mock(VkAccountRepository.class);
  RabbitClient events = mock(RabbitClient.class);
  VkMetrics metrics = new VkMetrics(new SimpleMeterRegistry());
  RaidStartHandler handler = new RaidStartHandler(
    new AccountEventForwarder(accountRepository, events, metrics)
  );

  @Test
  public void testTypeIsRaidStart() {
    assertEquals(VkEventType.RAID_START, handler.type());
  }

  @Test
  public void testSendingRaidEvent() throws Exception {
    var account = account("29564758", "recipient-123");
    when(accountRepository.findByVKId("29564758"))
      .thenReturn(List.of(account));

    assertNull(handler.handle(raidEvent()).join());

    var captor = ArgumentCaptor.forClass(VKLiveRaidEvent.class);

    verify(events).sendEvent(captor.capture());

    var sent = captor.getValue();
    assertEquals("7157765", sent.id());
    assertEquals("recipient-123", sent.recipientId());
    assertEquals("xkemurix", sent.username());
    assertEquals(71L, sent.raidersCount());
    assertEquals(Instant.ofEpochSecond(1786024445L), sent.startedAt());
  }

  @Test
  public void testNotSendingWithoutAccount() throws Exception {
    when(accountRepository.findByVKId("29564758")).thenReturn(List.of());

    assertNull(handler.handle(raidEvent()).join());

    verify(events, never()).sendEvent(any());
  }

  @Test
  public void testNotSendingWithoutRaid() throws Exception {
    var account = account("29564758", "recipient-123");
    when(accountRepository.findByVKId("29564758"))
      .thenReturn(List.of(account));

    var event = new VkWebhook.Event(
      "7157765",
      "raid_start",
      "29564758",
      new VkWebhook.EventData(
        demand(),
        follow(),
        null,
        null,
        null,
        null,
        new VkWebhook.Channel(8649069L, "mrwhiskanson"),
        new VkWebhook.Owner(29564758L)
      )
    );

    assertNull(handler.handle(event).join());

    verify(events, never()).sendEvent(any());
  }

  @Test
  public void testNotSendingWithoutRaidSource() throws Exception {
    stubAccount();
    assertNull(
      handler
        .handle(eventWith(new VkWebhook.Raid(71L, 1786024445L, null)))
        .join()
    );
    verify(events, never()).sendEvent(any());
  }

  @Test
  public void testNotSendingWithoutRaidOwner() throws Exception {
    stubAccount();
    var event = eventWith(
      new VkWebhook.Raid(
        71L,
        1786024445L,
        new VkWebhook.RaidSource(
          null,
          new VkWebhook.Channel(598281L, "xkemurix")
        )
      )
    );
    assertNull(handler.handle(event).join());
    verify(events, never()).sendEvent(any());
  }

  @Test
  public void testNotSendingWithoutRaidStartedAt() throws Exception {
    stubAccount();
    assertNull(
      handler
        .handle(eventWith(new VkWebhook.Raid(71L, null, raidSource())))
        .join()
    );
    verify(events, never()).sendEvent(any());
  }

  private void stubAccount() {
    var account = account("29564758", "recipient-123");
    when(accountRepository.findByVKId("29564758"))
      .thenReturn(List.of(account));
  }

  private VkWebhook.Event eventWith(VkWebhook.@Nullable Raid raid) {
    return new VkWebhook.Event(
      "7157765",
      "raid_start",
      "29564758",
      new VkWebhook.EventData(
        demand(),
        follow(),
        null,
        null,
        null,
        raid,
        new VkWebhook.Channel(8649069L, "mrwhiskanson"),
        new VkWebhook.Owner(29564758L)
      )
    );
  }

  private VkWebhook.RaidSource raidSource() {
    return new VkWebhook.RaidSource(
      new VkWebhook.RaidOwner(1, "https://avatar", "xkemurix", 29546516L),
      new VkWebhook.Channel(598281L, "xkemurix")
    );
  }

  private VkWebhook.Event raidEvent() {
    var owner = new VkWebhook.RaidOwner(
      1,
      "https://images.live.vkvideo.ru/user/29546516/avatar?change_time=1733401292",
      "xkemurix",
      29546516L
    );
    var source = new VkWebhook.RaidSource(
      owner,
      new VkWebhook.Channel(598281L, "xkemurix")
    );
    var raid = new VkWebhook.Raid(71L, 1786024445L, source);
    var data = new VkWebhook.EventData(
      demand(),
      follow(),
      null,
      null,
      null,
      raid,
      new VkWebhook.Channel(8649069L, "mrwhiskanson"),
      new VkWebhook.Owner(29564758L)
    );
    return new VkWebhook.Event("7157765", "raid_start", "29564758", data);
  }

}
