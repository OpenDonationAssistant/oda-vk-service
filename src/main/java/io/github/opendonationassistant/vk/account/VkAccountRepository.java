package io.github.opendonationassistant.vk.account;

import jakarta.inject.Singleton;

@Singleton
public class VkAccountRepository {

  private final VkAccountDataRepository dataRepository;

  public VkAccountRepository(VkAccountDataRepository dataRepository) {
    this.dataRepository = dataRepository;
  }

  public VkAccount create(
    String recipientId,
    String refreshTokenId,
    String vkId
  ) {
    var data = new VkAccountData(recipientId, refreshTokenId, vkId);
    var saved = dataRepository.save(data);
    return convert(saved);
  }

  private VkAccount convert(VkAccountData data) {
    return new VkAccount(data);
  }
}
