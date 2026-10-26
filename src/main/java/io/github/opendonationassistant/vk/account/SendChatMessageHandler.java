package io.github.opendonationassistant.vk.account;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Map;

/**
 * Consumes {@code SendVKLiveMessageCommand}. Wiring is in place (exchange
 * binding, handler registration, payload contract) but the outbound transport
 * is a TODO.
 *
 * <p>VK Live (vkvideo.ru) does not document a public API for sending stream-chat
 * messages. VK's {@code messages.send} belongs to the vk.com messenger, not the
 * stream chat, so it is not used here. The real transport (an undocumented
 * websocket/session protocol) must be reverse-engineered and isolated behind this
 * handler before the action can actually deliver a message.
 */
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
    // TODO(chat): implement VK Live chat send transport.
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
