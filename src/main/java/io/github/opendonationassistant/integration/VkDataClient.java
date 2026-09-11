package io.github.opendonationassistant.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Get;
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
    @QueryValue("channel_url") String channelUrl,
    @QueryValue("reward_id") String rewardId,
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

  @Get("/v1/channel")
  CompletableFuture<DataWrapper<ChannelInfo>> getChannelInfo(
    @Header("Authorization") String token,
    @QueryValue("channel_url") String channelUrl
  );

  @Serdeable
  public static record DataWrapper<T>(T data) {}

  @Serdeable
  public static record CreateRewardResponse(RewardData reward) {}

  @Serdeable
  public static record RewardData(String id) {}

  @Serdeable
  public static record ChannelInfo(
    @JsonProperty("channel") @Nullable Channel channel,
    @JsonProperty("owner") @Nullable Owner owner,
    @JsonProperty("stream") @Nullable Stream stream,
    @JsonProperty("streams") @Nullable List<Stream> streams
  ) {}

  @Serdeable
  public static record Channel(
    @JsonProperty("id") long id,
    @JsonProperty("nick") @Nullable String nick,
    @JsonProperty("nick_color") long nickColor,
    @JsonProperty("avatar_url") @Nullable String avatarUrl,
    @JsonProperty("cover_url") @Nullable String coverUrl,
    @JsonProperty("description") @Nullable String description,
    @JsonProperty("status") @Nullable String status,
    @JsonProperty("url") @Nullable String url,
    @JsonProperty("counters") @Nullable ChannelCounters counters,
    @JsonProperty("web_socket_channels") @Nullable WebSocketChannels webSocketChannels
  ) {}

  @Serdeable
  public static record ChannelCounters(@JsonProperty("subscribers") long subscribers) {}

  @Serdeable
  public static record WebSocketChannels(
    @JsonProperty("channel_points") @Nullable String channelPoints,
    @JsonProperty("chat") @Nullable String chat,
    @JsonProperty("info") @Nullable String info,
    @JsonProperty("limited_chat") @Nullable String limitedChat,
    @JsonProperty("limited_private_chat") @Nullable String limitedPrivateChat,
    @JsonProperty("private_channel_points") @Nullable String privateChannelPoints,
    @JsonProperty("private_chat") @Nullable String privateChat,
    @JsonProperty("private_info") @Nullable String privateInfo
  ) {}

  @Serdeable
  public static record Owner(
    @JsonProperty("id") long id,
    @JsonProperty("nick") @Nullable String nick,
    @JsonProperty("nick_color") long nickColor,
    @JsonProperty("avatar_url") @Nullable String avatarUrl,
    @JsonProperty("is_verified_streamer") boolean isVerifiedStreamer,
    @JsonProperty("external_profile_links") @Nullable List<ExternalProfileLink> externalProfileLinks
  ) {}

  @Serdeable
  public static record ExternalProfileLink(
    @JsonProperty("id") @Nullable String id,
    @JsonProperty("type") @Nullable String type
  ) {}

  @Serdeable
  public static record Stream(
    @JsonProperty("id") @Nullable String id,
    @JsonProperty("title") @Nullable String title,
    @JsonProperty("status") @Nullable String status,
    @JsonProperty("started_at") long startedAt,
    @JsonProperty("ended_at") long endedAt,
    @JsonProperty("preview_url") @Nullable String previewUrl,
    @JsonProperty("video_id") @Nullable String videoId,
    @JsonProperty("vk_video") @Nullable VkVideo vkVideo,
    @JsonProperty("category") @Nullable Category category,
    @JsonProperty("counters") @Nullable StreamCounters counters,
    @JsonProperty("reactions") @Nullable List<Reaction> reactions,
    @JsonProperty("slot") @Nullable Slot slot,
    @JsonProperty("source_urls") @Nullable List<SourceUrl> sourceUrls
  ) {}

  @Serdeable
  public static record VkVideo(
    @JsonProperty("owner_id") @Nullable String ownerId,
    @JsonProperty("video_id") @Nullable String videoId
  ) {}

  @Serdeable
  public static record Category(
    @JsonProperty("id") @Nullable String id,
    @JsonProperty("title") @Nullable String title,
    @JsonProperty("type") @Nullable String type,
    @JsonProperty("cover_url") @Nullable String coverUrl
  ) {}

  @Serdeable
  public static record StreamCounters(
    @JsonProperty("viewers") long viewers,
    @JsonProperty("views") long views
  ) {}

  @Serdeable
  public static record Reaction(
    @JsonProperty("type") @Nullable String type,
    @JsonProperty("count") long count
  ) {}

  @Serdeable
  public static record Slot(
    @JsonProperty("id") long id,
    @JsonProperty("url") @Nullable String url
  ) {}

  @Serdeable
  public static record SourceUrl(
    @JsonProperty("type") @Nullable String type,
    @JsonProperty("url") @Nullable String url
  ) {}

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
