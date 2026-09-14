package dev.victorloures.ledger.repository;

import dev.victorloures.ledger.domain.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {
}
