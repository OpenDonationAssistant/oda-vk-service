package io.github.opendonationassistant.vk.account;

import com.fasterxml.uuid.Generators;
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
    String username,
    String channelUrl
  ) {
    var data = new VkAccountData(
      Generators.timeBasedEpochGenerator().generate().toString(),
      vkId,
      username,
      channelUrl,
      recipientId,
      refreshTokenId
    );
    var saved = dataRepository.save(data);
    return convert(saved);
  }

  public List<VkAccount> findByRecipientId(String recipientId) {
    var data = dataRepository.findByRecipientId(recipientId);
    return data.stream().map(this::convert).toList();
  }

  public void deleteByRecipientIdAndRefreshTokenId(
    String recipientId,
    String refreshTokenId
  ) {
    dataRepository.deleteByRecipientIdAndRefreshTokenId(
      recipientId,
      refreshTokenId
    );
  }

  private VkAccount convert(VkAccountData data) {
    return new VkAccount(data);
  }
}
