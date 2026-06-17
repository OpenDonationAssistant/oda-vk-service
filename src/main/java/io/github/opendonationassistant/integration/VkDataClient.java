package io.github.opendonationassistant.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.serde.annotation.Serdeable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

@Client("vklive-data")
@Header(name = "Content-Type", value = "application/json")
public interface VkDataClient {
  @Post("/v1/channel_point/reward/create")
  CompletableFuture<DataWrapper<CreateRewardResponse>> createReward(
    @Header("Authorization") String token,
    @QueryValue("channel_url") String channelUrl,
    @Body RewardRequest request
  );

  @Post("/v1/channel_point/reward/edit")
  CompletableFuture<Void> editReward(
    @Header("Authorization") String token,
    @Body RewardRequest request
  );

  @Serdeable
  public static record AcceptRewardRequest(List<Demand> demands){}

  @Serdeable
  public static record Demand(Long id){}

  @Post("/v1/channel_point/reward/demand/accept")
  CompletableFuture<Void> acceptReward(
    @Header("Authorization") String token,
    @QueryValue("channel_url") String channelUrl,
    @Body AcceptRewardRequest request
  );

  @Post("/v1/channel_point/reward/delete")
  CompletableFuture<Void> deleteReward(
    @Header("Authorization") String token,
    @QueryValue("channel_url") String channelUrl,
    @QueryValue("reward_id") String rewardId
  );

  @Post("/v1/channel_point/reward/disable")
  CompletableFuture<Void> disableReward(
    @Header("Authorization") String token,
    @QueryValue("channel_url") String channelUrl,
    @QueryValue("reward_id") String rewardId
  );

  @Post("/v1/channel_point/reward/enable")
  CompletableFuture<Void> enableReward(
    @Header("Authorization") String token,
    @QueryValue("channel_url") String channelUrl,
    @QueryValue("reward_id") String rewardId
  );

  @Serdeable
  public static record DataWrapper<T>(T data) {}

  @Serdeable
  public static record CreateRewardResponse(RewardData reward) {}

  @Serdeable
  public static record RewardData(String id) {}

  @Serdeable
  public record RewardRequest(@JsonProperty("reward") @Nullable Reward reward) {
    @Serdeable
    public static record Reward(
      // @JsonProperty("background_color") int backgroundColor,
      // @JsonProperty("description") @Nullable String description,
      @JsonProperty("is_message_required") boolean isMessageRequired,
      // @JsonProperty("max_uses_count") int maxUsesCount,
      // @JsonProperty("max_uses_count_per_user") int maxUsesCountPerUser,
      @JsonProperty("name") @Nullable String name,
      @JsonProperty("price") int price
      // @JsonProperty("repair_timeout") int repairTimeout
    ) {}
  }
}
