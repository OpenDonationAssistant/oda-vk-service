package io.github.opendonationassistant.vk.webhook.handlers;

import io.github.opendonationassistant.integration.VkClient;
import io.github.opendonationassistant.integration.VkDataClient;
import io.github.opendonationassistant.rabbit.RabbitClient;
import io.github.opendonationassistant.vk.account.VkAccount;
import io.github.opendonationassistant.vk.account.VkAccountRepository;
import io.github.opendonationassistant.vk.webhook.VkEventHandler;
import io.github.opendonationassistant.vk.webhook.VkWebhook.Event;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Singleton
public class PointsDemandCreateHandler implements VkEventHandler {

  private final VkClient vk;
  private final VkAccountRepository accountRepository;
  private final RabbitClient commands;

  @Inject
  public PointsDemandCreateHandler(
    VkClient vk,
    VkAccountRepository accountRepository,
    @Named("commands") RabbitClient commands
  ) {
    this.vk = vk;
    this.accountRepository = accountRepository;
    this.commands = commands;
  }

  @Override
  public boolean canHandle(String type) {
    return "channel_points_reward_demand_create".equals(type);
  }

  @Override
  public CompletableFuture<?> handle(Event event) {
    final Optional<VkAccount> account = accountRepository
      .findByVKId(event.userId())
      .stream()
      .findFirst();
    var acceptRequest = new VkDataClient.AcceptRewardRequest(
      List.of(new VkDataClient.Demand(event.data().demand().id()))
    );
    return account
      .map(it -> {
        event
          .data()
          .demand()
          .messageParts()
          .forEach(part -> {
            part
              .keySet()
              .forEach(key -> {
                if ("link".equals(key)) {
                  Optional.ofNullable(part.get(key))
                    .map(p -> p.content())
                    .ifPresent(url ->
                      commands.sendCommand(
                        new AddMediaCommand(
                          url,
                          event.data().demand().user().nick(),
                          it.data().recipientId(),
                          "twitch"
                        )
                      )
                    );
                }
              });
          });
        return it.acceptReward(acceptRequest);
      })
      .orElseGet(() -> CompletableFuture.completedFuture(null));
  }

  @Serdeable
  public static record AddMediaCommand(
    String url,
    String requester,
    String recipientId,
    String system
  ) {}
}
