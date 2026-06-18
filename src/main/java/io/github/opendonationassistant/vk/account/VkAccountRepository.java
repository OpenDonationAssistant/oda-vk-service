package io.github.opendonationassistant.vk.account;

import com.fasterxml.uuid.Generators;
import io.github.opendonationassistant.integration.VkClient;
import io.github.opendonationassistant.vk.reward.RewardDataRepository;
import jakarta.inject.Singleton;
import java.util.List;

@Singleton
public class VkAccountRepository {

  private final VkAccountDataRepository dataRepository;
  private final RewardDataRepository rewardRepository;
  private final VkClient vk;

  public VkAccountRepository(
    VkAccountDataRepository dataRepository,
    RewardDataRepository rewardRepository,
    VkClient vk
  ) {
    this.dataRepository = dataRepository;
    this.rewardRepository = rewardRepository;
    this.vk = vk;
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

  public List<VkAccount> findByVKId(String vkId) {
    return dataRepository.findByVkId(vkId).stream().map(this::convert).toList();
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
    return new VkAccount(data, vk, rewardRepository);
  }
}
