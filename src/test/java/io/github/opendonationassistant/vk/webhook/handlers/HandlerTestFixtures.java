package io.github.opendonationassistant.vk.webhook.handlers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.commons.BlockingExecutor;
import io.github.opendonationassistant.rabbit.AsyncRabbitClient;
import io.github.opendonationassistant.rabbit.RabbitClient;
import io.github.opendonationassistant.vk.account.VkAccount;
import io.github.opendonationassistant.vk.account.VkAccountData;
import io.github.opendonationassistant.vk.webhook.VkWebhook;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

final class HandlerTestFixtures {

  static final BlockingExecutor BLOCKING_EXECUTOR = new BlockingExecutor(
    Executors.newVirtualThreadPerTaskExecutor()
  );

  private HandlerTestFixtures() {}

  static CompletableFuture<List<VkAccount>> found(VkAccount... accounts) {
    return CompletableFuture.completedFuture(List.of(accounts));
  }

  static AsyncRabbitClient eventSender(RabbitClient events) {
    return new AsyncRabbitClient(events, BLOCKING_EXECUTOR);
  }

  static VkAccount account(String vkId, String recipientId) {
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

  static VkWebhook.Demand demand() {
    return new VkWebhook.Demand(
      8433849L,
      new VkWebhook.User("stCarolas", 15346984L),
      new VkWebhook.Reward("39a919a7-fd10-4ece-9606-dde43b595fc9"),
      "pending",
      List.of()
    );
  }

  static VkWebhook.Follow follow() {
    return new VkWebhook.Follow(1785489039L, follower());
  }

  static VkWebhook.Follower follower() {
    return new VkWebhook.Follower(15, 19219592L, "OneTuz", "https://avatar");
  }

  static VkWebhook.Category category() {
    return new VkWebhook.Category("game", "Some Game", "cat-id");
  }
}
