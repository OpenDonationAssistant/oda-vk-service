package io.github.opendonationassistant.vk.account;

import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Singleton;
import java.io.IOException;

@Singleton
public class UnlinkAccountCommandHandler
  extends AbstractMessageHandler<UnlinkAccountCommandHandler.UnlinkVkAccount> {

  private final VkAccountRepository repository;

  public UnlinkAccountCommandHandler(
    ObjectMapper mapper,
    VkAccountRepository repository
  ) {
    super(mapper);
    this.repository = repository;
  }

  @Override
  public void handle(UnlinkVkAccount command) throws IOException {
    repository.deleteByRecipientIdAndRefreshTokenId(
      command.recipientId(),
      command.refreshTokenId()
    );
  }

  @Serdeable
  public static record UnlinkVkAccount(
    String recipientId,
    String refreshTokenId
  ) {}
}
