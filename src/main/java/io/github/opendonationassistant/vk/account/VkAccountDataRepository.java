package io.github.opendonationassistant.vk.account;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.List;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface VkAccountDataRepository
  extends CrudRepository<VkAccountData, String> {
  List<VkAccountData> findByRecipientId(String recipientId);

  void deleteByRecipientIdAndRefreshTokenId(
    String recipientId,
    String refreshTokenId
  );
}
