package com.example.httplogger.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpAppender extends AppenderBase<ILoggingEvent> {

    private WebClient webClient;
    private String endpoint;
    private int retries = 5; // Valor padrão de retentativas

    // Pool de threads para não bloquear a aplicação
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @Override
    public void start() {
        if (this.endpoint == null) {
            addError("Endpoint for HttpAppender is not set.");
            return;
        }
        this.webClient = WebClient.builder().baseUrl(this.endpoint).build();
        super.start();
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        // Não bloquear o thread da aplicação. Submete a tarefa para o pool.
        executorService.submit(() -> sendLog(eventObject));
    }

    private void sendLog(ILoggingEvent eventObject) {
        String jsonLog = formatLogToJson(eventObject);

        webClient.post()
            .uri("") // URI é relativo ao baseUrl
            .header("Content-Type", "application/json")
            .bodyValue(jsonLog)
            .retrieve()
            .toBodilessEntity()
            .retryWhen(Retry.backoff(retries, Duration.ofSeconds(2))
                .filter(this::isRetryable)
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    // Loga o erro final no console do sistema para depuração
                    System.err.println("Log event discarded after " + retries + " retries. Final error: " + retrySignal.failure().getMessage());
                    return retrySignal.failure(); // Descarta o evento
                }))
            .doOnError(error -> {
                // Este bloco é chamado se as retentativas se esgotarem
                // A exceção já foi "tratada" pelo onRetryExhaustedThrow, então não fazemos nada aqui
            })
            .subscribe(); // Inicia a chamada reativa
    }

    private boolean isRetryable(Throwable throwable) {
        // Lógica de quais erros devem causar uma retentativa.
        // Ex: erros de servidor (5xx) ou problemas de conectividade.
        if (throwable instanceof WebClientResponseException) {
            return ((WebClientResponseException) throwable).getStatusCode().is5xxServerError();
        }
        return true; // Tenta novamente para a maioria dos outros erros de rede
    }

    private String formatLogToJson(ILoggingEvent event) {
        // Formatação simples para JSON. Em um projeto real, use uma biblioteca como Jackson.
        return String.format("{\"timestamp\": \"%s\", " +
                             "\"level\": \"%s\", " +
                             "\"logger\": \"%s\", " +
                             "\"thread\": \"%s\", " +
                             "\"message\": \"%s\"}",
            new java.util.Date(event.getTimeStamp()),
            event.getLevel(),
            event.getLoggerName(),
            event.getThreadName(),
            event.getFormattedMessage().replace("\"", "\\\"") // Escapa aspas
        );
    }

    @Override
    public void stop() {
        executorService.shutdown();
        super.stop();
    }

    // Getters e Setters para injeção de configuração pelo Logback
    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }
}
