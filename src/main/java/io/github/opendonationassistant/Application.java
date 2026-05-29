package io.github.opendonationassistant;

import io.github.opendonationassistant.rabbit.AMQPConfiguration;
import io.github.opendonationassistant.rabbit.Exchange;
import io.micronaut.context.ApplicationContextBuilder;
import io.micronaut.context.ApplicationContextConfigurer;
import io.micronaut.context.annotation.ContextConfigurer;
import io.micronaut.context.annotation.Factory;
import io.micronaut.rabbitmq.connect.ChannelInitializer;
import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.info.*;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;

@OpenAPIDefinition(info = @Info(title = "oda-vk-service", version = "0.0"))
@Factory
public class Application {

  @ContextConfigurer
  public static class DefaultEnvironmentConfigurer
    implements ApplicationContextConfigurer {

    @Override
    public void configure(ApplicationContextBuilder builder) {
      builder.defaultEnvironments("standalone");
    }
  }

  public static void main(String[] args) {
    Micronaut.run(Application.class, args);
  }

  @Singleton
  public ChannelInitializer rabbitConfiguration() {
    return new AMQPConfiguration(
      List.of(
        CommandListener.BINDING,
        Exchange.Exchange(
          "changes.widgets",
          Map.of("music", CommandListener.QUEUE)
        )
      )
    );
  }
}
