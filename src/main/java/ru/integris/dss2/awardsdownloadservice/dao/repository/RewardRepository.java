package ru.integris.dss2.awardsdownloadservice.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.integris.dss2.awardsdownloadservice.dao.entity.Reward;

@Repository
public interface RewardRepository extends JpaRepository<Reward, Long> {
}