package io.github.opendonationassistant.vk.webhook.handlers;

import static io.github.opendonationassistant.vk.webhook.handlers.HandlerTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.github.opendonationassistant.vk.account.VkAccountRepository;
import io.github.opendonationassistant.vk.metrics.VkMetrics;
import io.github.opendonationassistant.vk.reward.VkReward;
import io.github.opendonationassistant.vk.webhook.VkEventType;
import io.github.opendonationassistant.vk.webhook.VkWebhook;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

public class PointsDemandCreateHandlerTest {

  private static final String RECIPIENT_ID = "recipient-123";
  private static final String REWARD_ID =
    "39a919a7-fd10-4ece-9606-dde43b595fc9";

  private final VkAccountRepository accountRepository = mock(
    VkAccountRepository.class
  );
  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final VkMetrics metrics = new VkMetrics(meterRegistry);
  private final PointsDemandCreateHandler handler =
    new PointsDemandCreateHandler(accountRepository, metrics);

  @Test
  public void testTypeIsChannelPointsRewardDemandCreate() {
    assertEquals(
      VkEventType.CHANNEL_POINTS_REWARD_DEMAND_CREATE,
      handler.type()
    );
  }

  @Test
  public void testAcceptsRewardOfAccount() {
    var account = account("123123123", RECIPIENT_ID);
    var reward = mock(VkReward.class);
    var demand = demand();
    when(accountRepository.findByVKId("123123123"))
      .thenReturn(List.of(account));
    when(account.reward(REWARD_ID)).thenReturn(Optional.of(reward));
    when(reward.accept(demand)).thenReturn(
      CompletableFuture.completedFuture(null)
    );

    assertNull(handler.handle(event(demand)).join());

    verify(account).reward(REWARD_ID);
    verify(reward).accept(demand);
  }

  @Test
  public void testCountsEventWithoutAccount() {
    when(accountRepository.findByVKId("123123123")).thenReturn(List.of());

    assertNull(handler.handle(event(demand())).join());

    assertEquals(
      1,
      eventCounter(VkMetrics.EVENTS_NOACCOUNT_METRIC_NAME)
    );
  }

  @Test
  public void testDoesNothingWhenRewardIsUnknown() {
    var account = account("123123123", RECIPIENT_ID);
    when(accountRepository.findByVKId("123123123"))
      .thenReturn(List.of(account));
    when(account.reward(REWARD_ID)).thenReturn(Optional.empty());

    assertNull(handler.handle(event(demand())).join());

    verify(account).reward(REWARD_ID);
    assertEquals(
      1,
      eventCounter(VkMetrics.EVENTS_UNKNOWN_REWARD_METRIC_NAME)
    );
    assertEquals(
      0,
      eventCounter(VkMetrics.EVENTS_NOACCOUNT_METRIC_NAME)
    );
  }

  @Test
  public void testDoesNothingWhenDemandHasNoReward() {
    var account = account("123123123", RECIPIENT_ID);
    when(accountRepository.findByVKId("123123123"))
      .thenReturn(List.of(account));
    var demand = new VkWebhook.Demand(
      8433849L,
      new VkWebhook.User("stCarolas", 15346984L),
      null,
      "pending",
      List.of()
    );

    assertNull(handler.handle(event(demand)).join());

    verify(account, never()).reward(anyString());
    assertEquals(
      0,
      eventCounter(VkMetrics.EVENTS_NOACCOUNT_METRIC_NAME)
    );
  }

  @Test
  public void testIgnoresNotPendingDemand() {
    var demand = new VkWebhook.Demand(
      8433849L,
      new VkWebhook.User("stCarolas", 15346984L),
      new VkWebhook.Reward(REWARD_ID),
      "accepted",
      List.of()
    );

    assertNull(handler.handle(event(demand)).join());

    verifyNoInteractions(accountRepository);
  }

  @Test
  public void testIgnoresMissingDemand() {
    assertNull(handler.handle(event(null)).join());

    verifyNoInteractions(accountRepository);
  }

  private double eventCounter(String metricName) {
    return meterRegistry
      .counter(
        metricName,
        VkMetrics.TYPE_TAG,
        "channel_points_reward_demand_create"
      )
      .count();
  }

  private VkWebhook.Event event(VkWebhook.@Nullable Demand demand) {
    return new VkWebhook.Event(
      "6795254",
      "channel_points_reward_demand_create",
      "123123123",
      new VkWebhook.EventData(
        demand,
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
