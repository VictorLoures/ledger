package dev.victorloures.ledger.service;

import dev.victorloures.ledger.domain.Job;
import dev.victorloures.ledger.dto.CreateJobRequest;
import dev.victorloures.ledger.exception.JobNotFoundException;
import dev.victorloures.ledger.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class JobService {

    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional
    public Job create(CreateJobRequest request) {
        Job job = new Job(UUID.randomUUID(), request.jobType(), request.payload(), request.scheduledAt());
        return jobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public Job findById(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Job> findAll() {
        return jobRepository.findAll();
    }

    @Transactional
    public Job cancel(UUID id) {
        Job job = findById(id);
        job.cancel();
        return job;
    }

    @Transactional
    public Job reschedule(UUID id, OffsetDateTime newScheduledAt) {
        Job job = findById(id);
        job.reschedule(newScheduledAt);
        return job;
    }
}
