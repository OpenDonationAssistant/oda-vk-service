package io.github.opendonationassistant.vk.webhook;

import org.jspecify.annotations.Nullable;

/**
 * The VK Live webhook event types this service knows how to handle.
 *
 * <p>The {@code wireName} is the value VK sends in the {@code type} field
 * of the webhook payload. Handlers refer to the enum constant, so the
 * reference is compile-checked; the wire names themselves are pinned by
 * {@code VkEventTypeTest}.
 */
public enum VkEventType {
  CHANNEL_FOLLOW_CREATE("channel_follow_create"),
  CHANNEL_POINTS_REWARD_DEMAND_CREATE("channel_points_reward_demand_create"),
  RAID_START("raid_start"),
  CHANNEL_STREAM_RECORD_NEW("channel_stream_record_new"),
  CHANNEL_STREAM_START("channel_stream_start"),
  CHANNEL_STREAM_STOP("channel_stream_stop"),
  CHANNEL_SUBSCRIPTION_RENEW("channel_subscription_renew");

  private final String wireName;

  VkEventType(String wireName) {
    this.wireName = wireName;
  }

  public String wireName() {
    return wireName;
  }

  /**
   * Resolves the enum for a raw webhook {@code type} value.
   *
   * @return the matching type, or {@code null} when the value is unknown
   */
  public static @Nullable VkEventType fromWireName(@Nullable String wireName) {
    for (var type : values()) {
      if (type.wireName.equals(wireName)) {
        return type;
      }
    }
    return null;
  }
}
