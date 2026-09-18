package dev.victorloures.ledger.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Só o front-end do Módulo 9 (servidor de dev do Vite) precisa disso — API e
// front rodam em origens/portas diferentes em desenvolvimento. Sem CORS
// liberado aqui, o navegador bloquearia toda chamada fetch() do React pra
// essa API por política de mesma origem.
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PATCH");
    }
}
