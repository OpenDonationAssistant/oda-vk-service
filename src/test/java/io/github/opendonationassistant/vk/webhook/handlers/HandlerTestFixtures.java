package io.github.opendonationassistant.vk.webhook.handlers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.vk.account.VkAccount;
import io.github.opendonationassistant.vk.account.VkAccountData;
import io.github.opendonationassistant.vk.webhook.VkWebhook;
import java.util.List;

/**
 * Shared fixtures for VK webhook handler tests.
 *
 * <p>These builders were previously copy-pasted into every handler test.
 */
final class HandlerTestFixtures {

  private HandlerTestFixtures() {}

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
      new VkWebhook.User("stCarolas"),
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
