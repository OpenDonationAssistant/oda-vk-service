package io.github.opendonationassistant.vk.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

public class VkEventTypeTest {

  @Test
  public void pinsWireNames() {
    assertEquals(
      VkEventType.CHANNEL_FOLLOW_CREATE,
      VkEventType.fromWireName("channel_follow_create")
    );
    assertEquals(
      VkEventType.CHANNEL_POINTS_REWARD_DEMAND_CREATE,
      VkEventType.fromWireName("channel_points_reward_demand_create")
    );
    assertEquals(
      VkEventType.RAID_START,
      VkEventType.fromWireName("raid_start")
    );
    assertEquals(
      VkEventType.CHANNEL_STREAM_RECORD_NEW,
      VkEventType.fromWireName("channel_stream_record_new")
    );
    assertEquals(
      VkEventType.CHANNEL_STREAM_START,
      VkEventType.fromWireName("channel_stream_start")
    );
    assertEquals(
      VkEventType.CHANNEL_STREAM_STOP,
      VkEventType.fromWireName("channel_stream_stop")
    );
    assertEquals(
      VkEventType.CHANNEL_SUBSCRIPTION_RENEW,
      VkEventType.fromWireName("channel_subscription_renew")
    );
  }

  @Test
  public void returnsNullForUnknownWireName() {
    assertNull(VkEventType.fromWireName("no_such_event"));
  }

  @Test
  public void returnsNullForNullWireName() {
    assertNull(VkEventType.fromWireName(null));
  }

  @Test
  public void wireNamesAreUnique() {
    var seen = new HashSet<String>();

    for (var type : VkEventType.values()) {
      assertTrue(
        seen.add(type.wireName()),
        "duplicate wire name: " + type.wireName()
      );
    }
  }
}
