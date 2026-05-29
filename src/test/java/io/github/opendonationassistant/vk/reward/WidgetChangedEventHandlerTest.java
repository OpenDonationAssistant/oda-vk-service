package io.github.opendonationassistant.vk.reward;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.instancio.junit.InstancioExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;

@MicronautTest
@ExtendWith(InstancioExtension.class)
@ExtendWith(MockServerExtension.class)
@MockServerSettings(ports = { 8080 })
public class WidgetChangedEventHandlerTest {

  private static ODALogger log = new ODALogger(
    WidgetChangedEventHandlerTest.class
  );

  @Test
  public void test() {}
}
