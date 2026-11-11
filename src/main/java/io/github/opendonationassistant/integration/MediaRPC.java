package io.github.opendonationassistant.integration;

import io.micronaut.rabbitmq.annotation.Binding;
import io.micronaut.rabbitmq.annotation.RabbitClient;
import io.micronaut.rabbitmq.annotation.RabbitProperty;
import io.micronaut.serde.annotation.Serdeable;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

@RabbitClient("rpc")
@RabbitProperty(name = "replyTo", value = "amq.rabbitmq.reply-to")
public interface MediaRPC {
  @Binding("media.add")
  CompletableFuture<@Nullable AddMediaResponse> add(AddMediaRequest request);

  @Serdeable
  public static record AddMediaRequest(
    String id,
    String url,
    String requester,
    String recipientId,
    String system
  ) {}

  @Serdeable
  public static record AddMediaResponse(
    @Nullable String id,
    @Nullable String message
  ) {}
}
