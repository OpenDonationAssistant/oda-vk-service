package io.github.opendonationassistant.vk.account;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Map;

@Singleton
public class SendChatMessageHandler
  extends AbstractMessageHandler<
    SendChatMessageHandler.SendVKLiveMessageCommand
  > {

  private final ODALogger log = new ODALogger(this);

  @Inject
  public SendChatMessageHandler(ObjectMapper mapper) {
    super(mapper);
  }

  // TODO(security): AbstractMessageHandler.handle(byte[]) logs the full
  // payload at INFO, which includes senderRefreshTokenId and user-authored
  // message text. Redact in oda-libraries, or drop senderRefreshTokenId
  // here once the token is resolved via TokenRPC from recipientId.
  @Override
  public void handle(SendVKLiveMessageCommand message) {
    // TODO(chat): resolve the account and forward the message through
    // VkClient#sendChatMessage.
    log.warn(
      "SendVKLiveMessageCommand is wired but not implemented yet",
      Map.of(
        "recipientId",
        message.recipientId(),
        "recipientVkId",
        message.recipientVkId()
      )
    );
  }

  @Serdeable
  public static record SendVKLiveMessageCommand(
    String recipientId,
    String senderRefreshTokenId,
    String recipientVkId,
    String message
  ) {}
}
