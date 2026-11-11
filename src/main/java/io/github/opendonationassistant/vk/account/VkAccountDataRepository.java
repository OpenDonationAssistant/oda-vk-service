package io.github.opendonationassistant.vk.account;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.async.AsyncCrudRepository;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface VkAccountDataRepository
  extends AsyncCrudRepository<VkAccountData, String> {
  CompletableFuture<List<VkAccountData>> findByRecipientId(
    String recipientId
  );

  CompletableFuture<List<VkAccountData>> findByVkId(String vkId);

  CompletableFuture<Long> deleteByRecipientIdAndRefreshTokenId(
    String recipientId,
    String refreshTokenId
  );
}
