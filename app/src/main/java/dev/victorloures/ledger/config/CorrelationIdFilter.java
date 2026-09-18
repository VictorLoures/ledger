package dev.victorloures.ledger.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

// Propaga (ou gera) um id de correlação por requisição HTTP, disponível em
// todo log emitido durante essa requisição via MDC — sem precisar passar
// esse id manualmente pra cada chamada de log. Aceita um id vindo de fora
// (cliente ou proxy) via header, senão gera um novo; sempre devolve o id
// usado na resposta, pra quem chamou conseguir cruzar com os logs do servidor.
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER_NAME);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER_NAME, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            // MDC é por thread; como o servlet container reutiliza threads
            // entre requisições, esquecer de limpar vazaria o id de uma
            // requisição pros logs da próxima que cair na mesma thread.
            MDC.remove(MDC_KEY);
        }
    }
}
