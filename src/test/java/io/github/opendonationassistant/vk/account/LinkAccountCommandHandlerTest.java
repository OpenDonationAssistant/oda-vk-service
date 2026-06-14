package io.github.opendonationassistant.vk.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
public class LinkAccountCommandHandlerTest {

  @Inject
  ChannelPool channel;

  @Inject
  ObjectMapper mapper;

  @Inject
  VkAccountRepository repository;

  @Test
  public void testCreatingLink() {
    var recipientId = Generators.timeBasedEpochGenerator()
      .generate()
      .toString();
    var refreshTokenId = Generators.timeBasedEpochGenerator()
      .generate()
      .toString();
    var rabbit = new RabbitClient(channel, mapper, "commands");
    rabbit.sendCommand(
      new LinkAccountCommandHandler.LinkVkAccount(
        recipientId,
        refreshTokenId,
        "id",
        "username",
        "channelUrl"
      )
    );
    Awaitility.await()
      .pollDelay(Duration.ofSeconds(1))
      .until(() -> repository.findByRecipientId(recipientId).size() > 0);
    var all = repository.findByRecipientId(recipientId);
    assertEquals(1, all.size());
    var account = all.stream().findFirst().get();
    assertTrue(account.data().id() != null);
    assertEquals(refreshTokenId, account.data().refreshTokenId());
    assertEquals("id", account.data().vkId());
    assertEquals("username", account.data().username());
    assertEquals("channelUrl", account.data().channelUrl());
  }
}
