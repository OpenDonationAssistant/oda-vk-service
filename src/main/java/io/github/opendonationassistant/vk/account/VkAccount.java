package io.github.opendonationassistant.vk.account;

import io.github.opendonationassistant.integration.VkClient;
import io.github.opendonationassistant.integration.VkDataClient;
import io.github.opendonationassistant.rabbit.RabbitClient;
import io.github.opendonationassistant.vk.reward.Reward;
import io.github.opendonationassistant.vk.reward.RewardData;
import io.github.opendonationassistant.vk.reward.RewardDataRepository;
import io.github.opendonationassistant.vk.webhook.VkWebhook;
import io.micronaut.serde.annotation.Serdeable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Serdeable
public class VkAccount {

  public static final String VK_LIVE_SYSTEM = "vklive";

  private final VkAccountData data;
  private final VkClient vk;
  private final RewardDataRepository rewardRepository;
  private final RabbitClient commands;

  public VkAccount(
    VkAccountData data,
    VkClient vk,
    RewardDataRepository rewardRepository,
    RabbitClient commands
  ) {
    this.data = data;
    this.vk = vk;
    this.rewardRepository = rewardRepository;
    this.commands = commands;
  }

  public VkAccountData data() {
    return data;
  }

  public CompletableFuture<Void> acceptReward(VkWebhook.Event event) {
    var demand = event.data().demand();
    if (demand == null) {
      return CompletableFuture.completedFuture(null);
    }
    var reward = demand.reward();
    var user = demand.user();
    var messageParts = demand.messageParts();
    if (reward == null || user == null || messageParts == null) {
      return CompletableFuture.completedFuture(null);
    }
    return rewardRepository
      .findById(reward.id())
      .flatMap(rewardData ->
        messageParts
          .stream()
          .flatMap(part ->
            part
              .keySet()
              .stream()
              .filter(key -> "link".equals(key))
              .flatMap(key ->
                Optional.ofNullable(part.get(key))
                  .map(p -> p.content())
                  .stream()
              )
          )
          .findFirst()
      )
      .map(link -> {
        commands.sendCommand(
          new AddMediaCommand(
            link,
            user.nick(),
            data().recipientId(),
            VK_LIVE_SYSTEM
          )
        );
        var acceptRequest = new VkDataClient.AcceptRewardRequest(
          List.of(new VkDataClient.Demand(demand.id()))
        );
        return acceptReward(acceptRequest);
      })
      .orElseGet(() -> CompletableFuture.<Void>completedFuture(null));
  }

  private CompletableFuture<Void> acceptReward(
    VkDataClient.AcceptRewardRequest request
  ) {
    return vk.acceptReward(
      data.recipientId(),
      data.refreshTokenId(),
      data.channelUrl(),
      request
    );
  }

  public Stream<Reward> getRewardsForWidget(String widgetId) {
    return rewardRepository
      .findByWidgetId(widgetId)
      .stream()
      .map(this::convert);
  }

  public CompletableFuture<Void> createReward(
    String widgetId,
    String type,
    String title,
    Integer cost
  ) {
    return vk
      .createReward(
        data.recipientId(),
        data.refreshTokenId(),
        data.channelUrl(),
        new VkDataClient.RewardRequest(
          new VkDataClient.RewardRequest.Reward(
            // 0,
            // null,
            true,
            // -1,
            // -1,
            title,
            cost
          )
        )
      )
      .thenAccept(id ->
        rewardRepository.save(new RewardData(id, data.id(), widgetId, type))
      );
  }

  private Reward convert(RewardData data) {
    return new Reward(data, this, vk);
  }

  @Serdeable
  public static record AddMediaCommand(
    String url,
    String requester,
    String recipientId,
    String system
  ) {}
}
