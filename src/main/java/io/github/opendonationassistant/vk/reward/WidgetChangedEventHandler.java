package io.github.opendonationassistant.vk.reward;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.integration.VkClient;
import io.github.opendonationassistant.integration.VkDataClient;
import io.github.opendonationassistant.rabbit.Exchange;
import io.github.opendonationassistant.vk.account.VkAccountRepository;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

@RabbitListener
public class WidgetChangedEventHandler {

  public static final String QUEUE_NAME = "vk.config-music";
  public static final io.github.opendonationassistant.rabbit.Queue QUEUE =
    new io.github.opendonationassistant.rabbit.Queue(QUEUE_NAME);
  public static final Exchange BINDING = Exchange.Exchange(
    "changes.widgets",
    Map.of("music", WidgetChangedEventHandler.QUEUE)
  );
  private static final String WIDGET_TYPE = "media";

  private ODALogger log = new ODALogger(this);

  private final TimeBasedEpochGenerator uuid =
    Generators.timeBasedEpochGenerator();
  private final RewardRepository rewardRepository;
  private final VkAccountRepository accountRepository;
  private final VkClient vk;

  @Inject
  public WidgetChangedEventHandler(
    RewardRepository rewardRepository,
    VkAccountRepository accountRepository,
    VkClient vk
  ) {
    this.rewardRepository = rewardRepository;
    this.accountRepository = accountRepository;
    this.vk = vk;
  }

  @Queue("vk.config-music")
  public void handle(WidgetChangedEvent event) throws IOException {
    if (!"updated".equals(event.type())) {
      return;
    }

    var widget = event.widget();
    if (widget == null) {
      return;
    }
    if (!WIDGET_TYPE.equals(widget.type())) {
      return;
    }

    var config = widget.config();
    if (config == null) {
      return;
    }

    var properties = config.properties();
    if (properties == null) {
      return;
    }

    var ownerId = widget.ownerId();
    if (ownerId == null) {
      return;
    }

    var accounts = accountRepository.findByRecipientId(ownerId);
    if (accounts.isEmpty()) {
      return;
    }
    accounts.forEach(account -> {
      // rewardRepository.deleteByRecipientId(recipientId);
      processSystem(properties, "vk", ownerId, account.data().refreshTokenId());
    });
  }

  private void processSystem(
    List<WidgetProperty> properties,
    String system,
    String recipientId,
    String refreshTokenId
  ) {
    var enabled = findBoolProperty(
      properties,
      system + "PointsRequestsEnabled"
    );
    log.info("music-" + system + "-request-title: " + enabled);
    if (!enabled) {
      return;
    }

    var title = findStringProperty(
      properties,
      "music-" + system + "-request-title"
    );
    log.info("music-" + system + "-request-title: " + title);
    if (title == null) {
      return;
    }
    Integer cost = findIntProperty(
      properties,
      "music-" + system + "-request-cost"
    );
    log.info("music-" + system + "-request-cost: " + cost);
    if (cost == null) {
      return;
    }
    vk.createReward(
      new VkDataClient.RewardRequest(
        new VkDataClient.RewardRequest.Reward(
          0,
          null,
          true,
          -1,
          -1,
          title,
          cost,
          -1
        )
      )
    );
    rewardRepository.save(
      new RewardData(
        uuid.generate().toString(),
        recipientId,
        refreshTokenId,
        "music"
      )
    );
  }

  private boolean findBoolProperty(
    List<WidgetProperty> properties,
    String name
  ) {
    return properties
      .stream()
      .filter(p -> name.equals(p.name()))
      .findFirst()
      .map(WidgetProperty::value)
      .map(v -> Boolean.TRUE.equals(v))
      .orElse(false);
  }

  private @Nullable String findStringProperty(
    List<WidgetProperty> properties,
    String name
  ) {
    return properties
      .stream()
      .filter(p -> name.equals(p.name()))
      .findFirst()
      .map(WidgetProperty::value)
      .map(Object::toString)
      .orElse(null);
  }

  private @Nullable Integer findIntProperty(
    List<WidgetProperty> properties,
    String name
  ) {
    return properties
      .stream()
      .filter(p -> name.equals(p.name()))
      .findFirst()
      .map(WidgetProperty::value)
      .filter(v -> v instanceof Number)
      .map(v -> ((Number) v).intValue())
      .orElse(null);
  }

  @Serdeable
  public static record WidgetChangedEvent(
    String type,
    Widget widget,
    String source,
    @Nullable String originId
  ) {}

  @Serdeable
  public static record Widget(
    String id,
    String type,
    Integer sortOrder,
    String name,
    Boolean enabled,
    String ownerId,
    WidgetConfig config
  ) {}

  @Serdeable
  public static record WidgetConfig(List<WidgetProperty> properties) {}

  @Serdeable
  public static record WidgetProperty(
    String name,
    @Nullable String displayName,
    @Nullable String type,
    @Nullable Object value
  ) {}
}
