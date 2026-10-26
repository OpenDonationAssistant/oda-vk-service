package io.github.opendonationassistant.vk.account;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.opendonationassistant.vk.account.SendChatMessageHandler.SendVKLiveMessageCommand;
import io.micronaut.serde.ObjectMapper;
import org.junit.jupiter.api.Test;

public class SendChatMessageHandlerTest {

  private final SendChatMessageHandler handler = new SendChatMessageHandler(
    ObjectMapper.getDefault()
  );

  @Test
  public void consumesCommandWithoutThrowing() {
    assertEquals("SendVKLiveMessageCommand", handler.type());
    handler.handle(
      new SendVKLiveMessageCommand("rec", "rt-1", "vk-target", "hello")
    );
  }
}
