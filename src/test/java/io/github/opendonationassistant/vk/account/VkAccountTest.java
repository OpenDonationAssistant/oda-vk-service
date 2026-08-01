package io.github.opendonationassistant.vk.account;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.opendonationassistant.integration.VkClient;
import io.github.opendonationassistant.rabbit.RabbitClient;
import io.github.opendonationassistant.vk.reward.RewardDataRepository;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.instancio.junit.InstancioExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@MicronautTest
@ExtendWith(InstancioExtension.class)
public class VkAccountTest {

  String rewardEvent =
    "{\"legacy_user_id\":15346984,\"data\":{\"channel\":{\"id\":15346984,\"url\":\"stcarolas\"},\"demand\":{\"status\":\"pending\",\"reward\":{\"id\":\"39a919a7-fd10-4ece-9606-dde43b595fc9\",\"name\":\"Реквест музыки\",\"price\":100},\"message_parts\":[{\"link\":{\"url\":\"https://ya.ru\",\"content\":\"https://ya.ru\"}},{\"text\":{\"content\":\"\\n\"}}],\"id\":8433849,\"created_at\":1784659721,\"user\":{\"nick_color\":6,\"avatar_url\":\"https://images.live.vkvideo.ru/user/15346984/avatar?change_time=1726744429\",\"nick\":\"stCarolas\",\"id\":15346984}},\"owner\":{\"id\":29586153}},\"type\":\"channel_points_reward_demand_create\",\"user_id\":29586153,\"id\":\"6795254\",\"time\":1784659721}";

  VkClient vkClient = mock(VkClient.class);
  RewardDataRepository rewardRepository = mock(RewardDataRepository.class);
  RabbitClient commands = mock(RabbitClient.class);
  

  @Test
  public void testNotHandlingUnknownReward() {
  // VkAccount = VkAccount(
  //   VkAccountData data,
  //   VkClient vk,
  //   RewardDataRepository rewardRepository,
  //   RabbitClient commands
  // ) {
  }
}
