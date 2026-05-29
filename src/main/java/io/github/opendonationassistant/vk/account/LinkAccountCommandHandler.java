package io.github.opendonationassistant.vk.account;

import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Singleton;
import java.io.IOException;

@Singleton
public class LinkAccountCommandHandler
  extends AbstractMessageHandler<LinkAccountCommandHandler.LinkTwitchAccount> {

  private final VkAccountRepository repository;

  public LinkAccountCommandHandler(
    ObjectMapper mapper,
    VkAccountRepository repository
  ) {
    super(mapper);
    this.repository = repository;
  }

  @Override
  public void handle(LinkTwitchAccount command) throws IOException {
    repository.create(
      command.recipientId(),
      command.refreshTokenId(),
      command.id()
    );
  }

  @Serdeable
  public static record LinkTwitchAccount(
    String recipientId,
    String refreshTokenId,
    String id
  ) {}
}
