# Spring Boot: HTTP Log Appender PoC

Este é um projeto de Prova de Conceito (PoC) que demonstra como enviar logs de uma aplicação Spring Boot para um destino HTTP (como um coletor de logs ou um serviço de observabilidade) de forma assíncrona e resiliente.

O objetivo principal é validar o funcionamento de uma estratégia de logging "push", onde a aplicação é responsável por enviar seus logs para um endpoint central, em vez de escrever em arquivos de disco ou na saída padrão (`stdout`).

## Funcionalidades Principais

- **`HttpAppender.java`**: Um Appender customizado para o Logback (o sistema de logs padrão do Spring Boot).
- **Envio Assíncrono**: O envio de logs é feito em um pool de threads separado para não bloquear a thread principal da aplicação, garantindo que o desempenho da aplicação não seja afetado por um coletor de logs lento ou indisponível.
- **Lógica de Retentativas (Retry)**: Utiliza o `WebClient` do Spring WebFlux para tentar reenviar um log em caso de falha na comunicação com o endpoint. Por padrão, ele tenta 5 vezes com um backoff exponencial.
- **Descarte Seguro**: Se todas as tentativas de envio falharem, o evento de log é descartado silenciosamente para evitar o consumo de memória ou o bloqueio da aplicação.

---

## Como Configurar

A configuração do destino dos logs é feita no arquivo `src/main/resources/logback-spring.xml`.

Para alterar o endpoint para onde os logs serão enviados, modifique a tag `<endpoint>` dentro do appender `HTTP`.

```xml
<appender name="HTTP" class="com.example.httplogger.logging.HttpAppender">
    <!-- Modifique a URL abaixo para o seu coletor de logs -->
    <endpoint>http://localhost:8081/log</endpoint>
    
    <!-- (Opcional) Altere o número de retentativas -->
    <retries>5</retries>
</appender>
```

---

## Como Executar e Testar

### 1. Crie um Servidor Falso para Receber os Logs

Para testar o envio, você precisa de um serviço escutando no endpoint configurado. A maneira mais simples de fazer isso é usando a ferramenta `netcat` (`nc`), que cria um servidor TCP básico e imprime tudo o que recebe no console.

Abra um terminal e execute o seguinte comando. Ele ficará aguardando conexões na porta `8081`:

```bash
nc -l -p 8081
```

### 2. Execute a Aplicação Spring Boot

Em outro terminal, navegue até a raiz do projeto e execute a aplicação usando o Maven:

```bash
mvn spring-boot:run
```

A aplicação irá iniciar na porta `8080`.

### 3. Gere Logs com o Script de Loop

Para simular um fluxo contínuo de logs, criamos o script `error_loop.sh`. Ele chama o endpoint `/error` da aplicação a cada 2 segundos, forçando a geração de um log de erro.

Primeiro, dê permissão de execução ao script:
```bash
chmod +x error_loop.sh
```

Agora, em um terceiro terminal, execute o script:
```bash
./error_loop.sh
```

### 4. Observe o Resultado

- No terminal onde o `netcat` está rodando, você verá os logs chegando em formato JSON a cada 2 segundos.
- No terminal onde o script de loop está rodando, você verá o status de cada chamada HTTP.
- No terminal da aplicação Spring, você verá os logs sendo impressos no console (pois mantivemos o `ConsoleAppender` ativo para depuração).

Para testar a resiliência, pare o `netcat` (`Ctrl+C`). O script continuará rodando, mas os logs não chegarão. A aplicação Spring continuará funcionando normalmente e, em seu console, você verá mensagens indicando que os logs estão sendo descartados após as tentativas de reenvio falharem.
