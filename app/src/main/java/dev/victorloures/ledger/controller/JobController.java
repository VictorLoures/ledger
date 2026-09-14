package dev.victorloures.ledger.controller;

import dev.victorloures.ledger.dto.CreateJobRequest;
import dev.victorloures.ledger.dto.JobResponse;
import dev.victorloures.ledger.service.JobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobResponse> create(@RequestBody CreateJobRequest request) {
        var job = jobService.create(request);
        return ResponseEntity.created(URI.create("/jobs/" + job.getId()))
                .body(JobResponse.from(job));
    }

    @GetMapping("/{id}")
    public JobResponse getById(@PathVariable UUID id) {
        return JobResponse.from(jobService.findById(id));
    }

    @GetMapping
    public List<JobResponse> getAll() {
        return jobService.findAll().stream()
                .map(JobResponse::from)
                .toList();
    }
}
