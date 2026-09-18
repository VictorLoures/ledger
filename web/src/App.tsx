import { useEffect, useMemo, useState } from "react";
import "./App.css";
import { cancelJob, createJob, listJobs, rescheduleJob, type Job, type JobStatus } from "./api";

const STATUS_OPTIONS: (JobStatus | "ALL")[] = ["ALL", "PENDING", "RUNNING", "DONE", "FAILED", "CANCELLED"];

// Pode cancelar/reagendar só nesses estados — espelha as mesmas regras que
// Job.cancel()/Job.reschedule() aplicam no back-end. Duplicar aqui evita uma
// chamada fadada a dar 409 só pra descobrir que o botão não deveria existir;
// o back-end continua sendo a fonte de verdade (ele valida de novo).
const CANCELLABLE: JobStatus[] = ["PENDING", "FAILED"];
const RESCHEDULABLE: JobStatus[] = ["PENDING", "FAILED", "CANCELLED"];

export default function App() {
  const [jobs, setJobs] = useState<Job[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<JobStatus | "ALL">("ALL");
  const [reschedulingId, setReschedulingId] = useState<string | null>(null);
  const [rescheduleDate, setRescheduleDate] = useState("");

  async function loadJobs() {
    setLoading(true);
    setError(null);
    try {
      setJobs(await listJobs());
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadJobs();
  }, []);

  const filteredJobs = useMemo(
    () => (statusFilter === "ALL" ? jobs : jobs.filter((j) => j.status === statusFilter)),
    [jobs, statusFilter],
  );

  async function handleCancel(id: string) {
    setError(null);
    try {
      await cancelJob(id);
      await loadJobs();
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function handleConfirmReschedule(id: string) {
    if (!rescheduleDate) return;
    setError(null);
    try {
      await rescheduleJob(id, new Date(rescheduleDate).toISOString());
      setReschedulingId(null);
      setRescheduleDate("");
      await loadJobs();
    } catch (e) {
      setError((e as Error).message);
    }
  }

  return (
    <main className="container">
      <h1>Ledger — Jobs</h1>

      <CreateJobForm onCreated={loadJobs} onError={setError} />

      <div className="toolbar">
        <label>
          Filtrar por status:{" "}
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value as JobStatus | "ALL")}>
            {STATUS_OPTIONS.map((s) => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>
        </label>
        <button onClick={loadJobs} disabled={loading}>Recarregar</button>
      </div>

      {error && <p className="error">Erro: {error}</p>}
      {loading && <p>Carregando...</p>}

      {!loading && filteredJobs.length === 0 && <p>Nenhum job encontrado.</p>}

      {!loading && filteredJobs.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Id</th><th>Tipo</th><th>Status</th><th>Agendado para</th><th>Tentativas</th><th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {filteredJobs.map((job) => (
              <tr key={job.id}>
                <td title={job.id}>{job.id.slice(0, 8)}…</td>
                <td>{job.jobType}</td>
                <td><span className={`badge badge-${job.status.toLowerCase()}`}>{job.status}</span></td>
                <td>{new Date(job.scheduledAt).toLocaleString()}</td>
                <td>{job.attempts}</td>
                <td className="actions">
                  {CANCELLABLE.includes(job.status) && (
                    <button onClick={() => handleCancel(job.id)}>Cancelar</button>
                  )}
                  {RESCHEDULABLE.includes(job.status) && (
                    reschedulingId === job.id ? (
                      <>
                        <input
                          type="datetime-local"
                          value={rescheduleDate}
                          onChange={(e) => setRescheduleDate(e.target.value)}
                        />
                        <button onClick={() => handleConfirmReschedule(job.id)}>Confirmar</button>
                        <button onClick={() => setReschedulingId(null)}>Cancelar edição</button>
                      </>
                    ) : (
                      <button onClick={() => setReschedulingId(job.id)}>Reagendar</button>
                    )
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </main>
  );
}

function CreateJobForm({ onCreated, onError }: { onCreated: () => void; onError: (msg: string) => void }) {
  const [jobType, setJobType] = useState("EMAIL");
  const [payload, setPayload] = useState('{"to":"teste@example.com"}');
  const [scheduledAt, setScheduledAt] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    onError("");
    try {
      const when = scheduledAt ? new Date(scheduledAt).toISOString() : new Date().toISOString();
      await createJob({ jobType, payload, scheduledAt: when });
      setScheduledAt("");
      onCreated();
    } catch (err) {
      onError((err as Error).message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="create-form" onSubmit={handleSubmit}>
      <h2>Novo job</h2>
      <label>
        Tipo:{" "}
        <select value={jobType} onChange={(e) => setJobType(e.target.value)}>
          <option value="EMAIL">EMAIL</option>
          <option value="WEBHOOK">WEBHOOK</option>
          <option value="REMINDER">REMINDER</option>
        </select>
      </label>
      <label>
        Payload (JSON):{" "}
        <input value={payload} onChange={(e) => setPayload(e.target.value)} />
      </label>
      <label>
        Agendado para (vazio = agora):{" "}
        <input type="datetime-local" value={scheduledAt} onChange={(e) => setScheduledAt(e.target.value)} />
      </label>
      <button type="submit" disabled={submitting}>Criar</button>
    </form>
  );
}
