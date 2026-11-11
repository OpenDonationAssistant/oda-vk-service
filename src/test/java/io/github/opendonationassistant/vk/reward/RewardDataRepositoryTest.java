package io.github.opendonationassistant.vk.reward;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.uuid.Generators;
import io.micronaut.data.exceptions.EmptyResultException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;

@MicronautTest(environments = "allinone")
public class RewardDataRepositoryTest {

  @Inject
  RewardDataRepository repository;

  @Test
  public void savesAndFindsRewardAsynchronously() {
    var id = Generators.timeBasedEpochGenerator().generate().toString();
    var widgetId = UUID.randomUUID().toString();
    var reward = new RewardData(
      id,
      UUID.randomUUID().toString(),
      widgetId,
      "music"
    );

    assertNotNull(repository.save(reward).join());

    var found = repository.findById(id).join();
    assertEquals("music", found.type());

    var optional = repository.findOneById(id).join();
    assertTrue(optional.isPresent());
    assertEquals("music", optional.orElseThrow().type());

    var byWidget = repository.findByWidgetId(widgetId).join();
    assertEquals(1, byWidget.size());
    assertEquals(id, byWidget.get(0).id());
  }

  @Test
  public void returnsEmptyFutureForUnknownReward() {
    assertTrue(
      repository.findOneById(UUID.randomUUID().toString()).join().isEmpty()
    );
  }

  @Test
  public void failsFindByIdForUnknownReward() {
    var error = assertThrows(
      CompletionException.class,
      () -> repository.findById(UUID.randomUUID().toString()).join()
    );

    assertInstanceOf(EmptyResultException.class, error.getCause());
  }
}
