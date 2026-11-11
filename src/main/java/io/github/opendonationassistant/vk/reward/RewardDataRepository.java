package io.github.opendonationassistant.vk.reward;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.async.AsyncCrudRepository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface RewardDataRepository
  extends AsyncCrudRepository<RewardData, String> {
  CompletableFuture<List<RewardData>> findByWidgetId(String widgetId);

  CompletableFuture<Optional<RewardData>> findOneById(String id);
}
