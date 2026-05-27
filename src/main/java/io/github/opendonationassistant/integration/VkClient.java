package io.github.opendonationassistant.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.serde.annotation.Serdeable;
import org.jspecify.annotations.Nullable;

@Client("${vk.api.url}")
public interface VkClient {
  @Post("/v1/channel_point/reward/create")
  void createReward(@Body RewardRequest request);

  @Post("/v1/channel_point/reward/edit")
  void editReward(@Body RewardRequest request);

  @Post("/v1/channel_point/reward/delete")
  void deleteReward(
    @QueryValue("channel_id") String channelId,
    @QueryValue("reward_id") String rewardId
  );

  @Post("/v1/channel_point/reward/disable")
  void disableReward(
    @QueryValue("channel_id") String channelId,
    @QueryValue("reward_id") String rewardId
  );

  @Post("/v1/channel_point/reward/enable")
  void enableReward(
    @QueryValue("channel_id") String channelId,
    @QueryValue("reward_id") String rewardId
  );

  @Serdeable
  public record RewardRequest(@JsonProperty("reward") @Nullable Reward reward) {
    @Serdeable
    public static record Reward(
      @JsonProperty("background_color") int backgroundColor,
      @JsonProperty("description") @Nullable String description,
      @JsonProperty("is_message_required") boolean isMessageRequired,
      @JsonProperty("max_uses_count") int maxUsesCount,
      @JsonProperty("max_uses_count_per_user") int maxUsesCountPerUser,
      @JsonProperty("name") @Nullable String name,
      @JsonProperty("price") int price,
      @JsonProperty("repair_timeout") int repairTimeout
    ) {}
  }
}
