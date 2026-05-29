package io.github.opendonationassistant.vk.reward;

import jakarta.inject.Singleton;
import java.util.List;
import java.util.Optional;

@Singleton
public class RewardRepository {

  private final RewardDataRepository dataRepository;

  public RewardRepository(RewardDataRepository dataRepository) {
    this.dataRepository = dataRepository;
  }

  public Reward save(RewardData data) {
    var saved = dataRepository.save(data);
    return new Reward(saved);
  }

  public Optional<Reward> getById(String id) {
    return dataRepository.findById(id).map(Reward::new);
  }

  public List<Reward> getAll() {
    return dataRepository.findAll().stream().map(Reward::new).toList();
  }

  public void delete(String id) {
    dataRepository.deleteById(id);
  }
}
