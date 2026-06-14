package io.github.opendonationassistant.vk.account;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.uuid.Generators;
import io.github.opendonationassistant.rabbit.RabbitClient;
import io.micronaut.rabbitmq.connect.ChannelPool;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.time.Duration;
import org.awaitility.Awaitility;
import org.instancio.junit.InstancioExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@MicronautTest(environments = "allinone")
@ExtendWith(InstancioExtension.class)
public class UnlinkAccountCommandHandlerTest {

  @Inject
  ChannelPool channel;

  @Inject
  ObjectMapper mapper;

  @Inject
  VkAccountRepository repository;

  @Test
  public void testUnlinkingAccount() {
    var recipientId = Generators.timeBasedEpochGenerator()
      .generate()
      .toString();
    var refreshTokenId = Generators.timeBasedEpochGenerator()
      .generate()
      .toString();
    var vkId = "vk-user-id";
    var username = "vk-username";
    var channelUrl = "channelUrl";

    repository.create(recipientId, refreshTokenId, vkId, username, channelUrl);
    var before = repository.findByRecipientId(recipientId);
    assertEquals(1, before.size());

    var rabbit = new RabbitClient(channel, mapper, "commands");
    rabbit.sendCommand(
      new UnlinkAccountCommandHandler.UnlinkVkAccount(
        recipientId,
        refreshTokenId
      )
    );

    Awaitility.await()
      .pollDelay(Duration.ofSeconds(1))
      .until(() -> repository.findByRecipientId(recipientId).isEmpty());
  }
}
