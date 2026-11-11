package io.github.opendonationassistant.vk.account;

import com.fasterxml.uuid.Generators;
import io.github.opendonationassistant.integration.MediaRPC;
import io.github.opendonationassistant.integration.VkClientFactory;
import io.github.opendonationassistant.vk.reward.RewardDataRepository;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

  public CompletableFuture<VkAccount> create(
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
    return dataRepository.save(data).thenApply(this::convert);
  }

  public CompletableFuture<List<VkAccount>> findByVKId(String vkId) {
    return dataRepository
      .findByVkId(vkId)
      .thenApply(data -> data.stream().map(this::convert).toList());
  }

  public CompletableFuture<List<VkAccount>> findByRecipientId(
    String recipientId
  ) {
    return dataRepository
      .findByRecipientId(recipientId)
      .thenApply(data -> data.stream().map(this::convert).toList());
  }

  public CompletableFuture<Void> deleteByRecipientIdAndRefreshTokenId(
    String recipientId,
    String refreshTokenId
  ) {
    return dataRepository
      .deleteByRecipientIdAndRefreshTokenId(recipientId, refreshTokenId)
      .thenAccept(deleted -> {});
  }

  private VkAccount convert(VkAccountData data) {
    return new VkAccount(data, vkClientFactory, rewardRepository, media);
  }
}
