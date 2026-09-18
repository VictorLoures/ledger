package dev.victorloures.ledger.controller;

import dev.victorloures.ledger.dto.CreateJobRequest;
import dev.victorloures.ledger.dto.JobResponse;
import dev.victorloures.ledger.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

// /api/v1: versionar desde já custa nada e evita quebrar clientes existentes
// no dia em que o contrato precisar mudar de forma incompatível (nesse caso,
// nasceria um /api/v2 rodando em paralelo, sem tocar no v1).
@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobResponse> create(@Valid @RequestBody CreateJobRequest request) {
        var job = jobService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/jobs/" + job.getId()))
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
