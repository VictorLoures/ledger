package dev.victorloures.ledger.config;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

// MDC é baseado em ThreadLocal: por padrão, o contexto (correlationId,
// jobId) que existe na thread que CHAMA @Async não é visto pela thread do
// pool que EXECUTA de fato — cada log dentro de processAsync sairia sem
// contexto nenhum. Este decorator captura o MDC no momento em que a tarefa é
// submetida (ainda na thread de quem chamou) e o reaplica dentro da thread
// do pool antes de rodar a tarefa de verdade.
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> callerContext = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            try {
                if (callerContext != null) {
                    MDC.setContextMap(callerContext);
                }
                runnable.run();
            } finally {
                if (previous != null) {
                    MDC.setContextMap(previous);
                } else {
                    MDC.clear();
                }
            }
        };
    }
}
