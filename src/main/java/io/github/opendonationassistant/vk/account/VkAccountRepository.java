package io.github.opendonationassistant.vk.account;

import com.fasterxml.uuid.Generators;
import io.github.opendonationassistant.integration.MediaRPC;
import io.github.opendonationassistant.integration.VkClientFactory;
import io.github.opendonationassistant.vk.reward.RewardDataRepository;
import jakarta.inject.Singleton;
import java.util.List;

@Singleton
public class VkAccountRepository {

  private final VkAccountDataRepository dataRepository;
  private final RewardDataRepository rewardRepository;
  private final VkClientFactory vkClientFactory;
  private final MediaRPC media;

  public VkAccountRepository(
    VkAccountDataRepository dataRepository,
    RewardDataRepository rewardRepository,
    VkClientFactory vkClientFactory,
    MediaRPC media
  ) {
    this.dataRepository = dataRepository;
    this.rewardRepository = rewardRepository;
    this.vkClientFactory = vkClientFactory;
    this.media = media;
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
    return new VkAccount(data, vkClientFactory, rewardRepository, media);
  }
}
