package io.github.opendonationassistant.vk.account;

import jakarta.inject.Singleton;
import java.util.List;

@Singleton
public class VkAccountRepository {

  private final VkAccountDataRepository dataRepository;

  public VkAccountRepository(VkAccountDataRepository dataRepository) {
    this.dataRepository = dataRepository;
  }

  public VkAccount create(
    String recipientId,
    String refreshTokenId,
    String vkId,
    String username
  ) {
    var data = new VkAccountData(vkId, username, recipientId, refreshTokenId);
    var saved = dataRepository.save(data);
    return convert(saved);
  }

  public List<VkAccount> findByRecipientId(String recipientId) {
    var data = dataRepository.findByRecipientId(recipientId);
    return data.stream().map(this::convert).toList();
  }

  private VkAccount convert(VkAccountData data) {
    return new VkAccount(data);
  }
}
