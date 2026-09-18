// URL fixa por simplicidade — é o "básico, só visualização" do Módulo 9,
// não vale a pena introduzir variáveis de ambiente (import.meta.env) pra
// uma única constante num projeto de estudo.
const BASE_URL = "http://localhost:8080/api/v1";

export type JobStatus = "PENDING" | "RUNNING" | "DONE" | "FAILED" | "CANCELLED";

export interface Job {
  id: string;
  jobType: "REMINDER" | "EMAIL" | "WEBHOOK";
  payload: string;
  status: JobStatus;
  scheduledAt: string;
  attempts: number;
}

export interface ApiError {
  status: number;
  message: string;
  errors?: { field: string; message: string }[] | null;
}

// O back-end (GlobalExceptionHandler) sempre devolve esse formato de erro,
// então um único ponto de tratamento aqui já cobre qualquer falha da API.
async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });

  if (!response.ok) {
    const body: ApiError = await response.json();
    throw new Error(body.errors?.length
      ? body.errors.map((e) => `${e.field}: ${e.message}`).join("; ")
      : body.message);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return response.json();
}

export function listJobs(): Promise<Job[]> {
  return request<Job[]>("/jobs");
}

export function createJob(input: { jobType: string; payload: string; scheduledAt: string }): Promise<Job> {
  return request<Job>("/jobs", { method: "POST", body: JSON.stringify(input) });
}

export function cancelJob(id: string): Promise<Job> {
  return request<Job>(`/jobs/${id}/cancel`, { method: "PATCH" });
}

export function rescheduleJob(id: string, scheduledAt: string): Promise<Job> {
  return request<Job>(`/jobs/${id}/reschedule`, {
    method: "PATCH",
    body: JSON.stringify({ scheduledAt }),
  });
}
