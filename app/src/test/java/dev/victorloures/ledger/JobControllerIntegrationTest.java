package dev.victorloures.ledger;

import dev.victorloures.ledger.domain.JobType;
import dev.victorloures.ledger.dto.CreateJobRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @SpringBootTest sobe o contexto Spring inteiro (controller -> service ->
// repository -> banco real via Testcontainers, migrado pelo Flyway) — testa
// a integração de verdade das camadas, não cada uma isolada com mock.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class JobControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsAndFetchesJob() throws Exception {
        var request = new CreateJobRequest(JobType.EMAIL, "{\"to\":\"teste@example.com\"}",
                OffsetDateTime.now());

        String response = mockMvc.perform(post("/api/v1/jobs")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/v1/jobs/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void returns404ForUnknownJob() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns400WithFieldErrorsForInvalidRequest() throws Exception {
        // payload em branco e scheduledAt ausente -> dois erros de campo
        var invalidJson = "{\"jobType\":\"EMAIL\",\"payload\":\"\"}";

        mockMvc.perform(post("/api/v1/jobs")
                        .contentType("application/json")
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[*].field", org.hamcrest.Matchers.hasItems("payload", "scheduledAt")));
    }

    @Test
    void cancelsAPendingJob() throws Exception {
        String id = createJob();

        mockMvc.perform(patch("/api/v1/jobs/" + id + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void reschedulesAJobResettingAttempts() throws Exception {
        String id = createJob();
        OffsetDateTime newDate = OffsetDateTime.now().plusDays(1);

        mockMvc.perform(patch("/api/v1/jobs/" + id + "/reschedule")
                        .contentType("application/json")
                        .content("{\"scheduledAt\":\"" + newDate + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.attempts").value(0));
    }

    @Test
    void cancellingAnAlreadyCancelledJobReturns409() throws Exception {
        String id = createJob();
        mockMvc.perform(patch("/api/v1/jobs/" + id + "/cancel")).andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/jobs/" + id + "/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    private String createJob() throws Exception {
        var request = new CreateJobRequest(JobType.EMAIL, "{\"to\":\"teste@example.com\"}", OffsetDateTime.now());
        String response = mockMvc.perform(post("/api/v1/jobs")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }
}
