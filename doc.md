Ótimo! Vamos começar analisando os potenciais problemas no fluxo básico, onde o Serviço A envia uma mensagem para o Serviço B:

### Problemas Comuns
1. **Falha no envio da mensagem**: O Serviço A pode enfrentar problemas ao publicar mensagens no Kafka (por exemplo, falhas de conectividade, configuração incorreta do cluster, ou indisponibilidade do broker).
2. **Duplicação de mensagens**: O Serviço A pode enviar a mesma mensagem mais de uma vez devido a falhas de rede ou comportamento "at least once".
3. **Indisponibilidade do consumidor**: O Serviço B pode estar indisponível no momento em que a mensagem é publicada.
4. **Erros de processamento**: O Serviço B pode encontrar erros ao tentar processar a mensagem, seja por problemas de lógica ou por dados inválidos.
5. **Perda de mensagens**: Mensagens podem ser perdidas se o consumidor não as processar antes de uma falha.
6. **Problemas de deserialização**: O formato da mensagem pode não ser compatível entre o produtor e o consumidor.

### Estratégias para Resiliência
#### 1. **Retries**
- **Produtor**: Configure `retries` no Kafka Producer para reenviar mensagens automaticamente em caso de falhas temporárias.
- **Consumidor**: Utilize uma lógica de retry personalizada para reprocessar mensagens que falharem no consumidor.
  
#### 2. **DLQ (Dead Letter Queue)**
- Configure uma fila dedicada para armazenar mensagens que falharem após um número configurável de tentativas de processamento. Isso evita a perda de dados e permite análises posteriores.

#### 3. **Idempotência**
- Configure o Kafka Producer para garantir idempotência (`enable.idempotence=true`). Assim, múltiplos envios da mesma mensagem não resultam em duplicações.

#### 4. **Timeouts e Circuit Breaker**
- No Serviço A, implemente um timeout para evitar bloqueios em caso de indisponibilidade do Kafka.
- No Serviço B, utilize um circuito breaker para pausar o consumo quando ocorrerem falhas repetidas.

#### 5. **Persistência Local**
- No Serviço A, armazene mensagens em um armazenamento local (como um banco de dados) antes de enviá-las para o Kafka, para garantir persistência em caso de falha.

#### 6. **Deserialização Robusta**
- Valide o esquema das mensagens usando ferramentas como Avro ou JSON Schema antes do envio e durante o consumo.

### Próximos Passos
1. Criar uma implementação inicial do fluxo básico com Spring Boot:
   - Serviço A (producer) publica mensagens no Kafka.
   - Serviço B (consumer) consome e processa as mensagens.

2. Adicionar resiliência incrementalmente:
   - Configurar retries no produtor e no consumidor.
   - Implementar DLQ para mensagens não processáveis.
   - Garantir idempotência no produtor.

Você quer começar com a configuração inicial do fluxo básico ou já introduzir alguma técnica de resiliência?




Perfeito! Vamos começar com o fluxo mais básico, onde o Serviço A envia uma mensagem para o Kafka, e o Serviço B consome e processa essa mensagem. Isso será o "Hello World" da nossa arquitetura.

### Passo 1: Configuração Inicial

#### 1. **Pré-requisitos**
Certifique-se de ter o seguinte:
- **Kafka**: Um cluster Kafka em execução (pode ser local usando `docker-compose`).
- **Spring Boot**: Dois projetos Spring Boot configurados com dependências do Kafka.

Adicione as dependências necessárias no `pom.xml` de ambos os serviços:

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
</dependencies>
```

#### 2. **Configuração do Kafka no `application.yml`**

##### Serviço A (Producer)
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
```

##### Serviço B (Consumer)
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: service-b-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      auto-offset-reset: earliest
```

#### 3. **Produção e Consumo de Mensagens**

##### Serviço A: Producer
```java
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProducerController {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public ProducerController(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @GetMapping("/send")
    public String sendMessage() {
        kafkaTemplate.send("example-topic", "Hello, Kafka!");
        return "Message sent!";
    }
}
```

##### Serviço B: Consumer
```java
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class MessageConsumer {
    @KafkaListener(topics = "example-topic", groupId = "service-b-group")
    public void consume(String message) {
        System.out.println("Received message: " + message);
    }
}
```

### Passo 2: Testar o Fluxo
1. Inicie o Kafka localmente.
2. Suba o Serviço B para que ele esteja ouvindo no tópico `example-topic`.
3. Suba o Serviço A e acesse a URL `http://localhost:8080/send` para disparar a mensagem.
4. Verifique os logs do Serviço B para garantir que a mensagem foi recebida.

Esse é o fluxo básico funcionando. Queremos partir daqui para algum ponto específico na evolução?




Ótimo! Com o fluxo básico funcionando, podemos começar a adicionar resiliência de forma incremental. Aqui estão algumas direções possíveis para o próximo passo:

1. **Retries no Consumidor**: Implementar tentativas de reprocessamento para lidar com falhas temporárias ao processar uma mensagem.
2. **DLQ (Dead Letter Queue)**: Adicionar uma fila para mensagens que falharem após múltiplas tentativas.
3. **Garantir Idempotência no Producer**: Configurar idempotência para evitar duplicação de mensagens.
4. **Persistência Local no Producer**: Garantir que mensagens sejam armazenadas localmente antes de serem enviadas ao Kafka.

Qual dessas melhorias você prefere implementar agora?


Perfeito! Vamos tratar o cenário de **falhas no envio da mensagem pelo producer**, garantindo que ele lide com problemas como indisponibilidade do broker ou falhas temporárias de rede.

### Passo 1: Configuração de Retries no Producer
O Kafka Producer oferece uma funcionalidade nativa para reenvio automático de mensagens em caso de falha.

#### Ajuste no `application.yml` do Serviço A
```yaml
spring:
  kafka:
    producer:
      retries: 5  # Número de tentativas de reenvio
      acks: all  # Garante que a mensagem será confirmada por todos os brokers replicados
      linger-ms: 5  # Opcional: Reduz a latência ao agrupar mensagens
      batch-size: 16384  # Opcional: Configuração de tamanho de batch para otimização
```

#### Explicação
- `retries`: O número de vezes que o producer tentará reenviar uma mensagem antes de falhar.
- `acks`: Define o nível de confirmação requerido. O valor `all` garante que todos os brokers replicados confirmem o recebimento antes de prosseguir.
- `linger-ms` e `batch-size`: Melhoram a performance enviando mensagens em lotes, mas são opcionais.

---

### Passo 2: Tratamento de Exceções no Producer
Mesmo com retries configurados, é essencial lidar com exceções no caso de falhas persistentes.

#### Ajuste no Producer com um Callback
```java
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ReliableProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public ReliableProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String topic, String message) {
        kafkaTemplate.send(topic, message).addCallback(
            result -> {
                if (result != null) {
                    RecordMetadata metadata = result.getRecordMetadata();
                    System.out.println("Message sent successfully to " +
                            metadata.topic() + " partition " + metadata.partition() +
                            " at offset " + metadata.offset());
                }
            },
            ex -> {
                System.err.println("Failed to send message: " + ex.getMessage());
                // Opcional: persistir a mensagem localmente para retry posterior
            }
        );
    }
}
```

---

### Passo 3: Persistência Local para Garantia
Se o producer falhar após todas as tentativas, você pode armazenar a mensagem localmente para reenvio manual ou automático mais tarde.

#### Exemplo usando um Banco de Dados Local
```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FailedMessagesRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void saveFailedMessage(String topic, String message) {
        jdbcTemplate.update("INSERT INTO failed_messages (topic, message) VALUES (?, ?)", topic, message);
    }
}
```

#### Integração com o Producer
```java
public void sendMessage(String topic, String message) {
    kafkaTemplate.send(topic, message).addCallback(
        result -> System.out.println("Message sent successfully!"),
        ex -> {
            System.err.println("Failed to send message: " + ex.getMessage());
            failedMessagesRepository.saveFailedMessage(topic, message);
        }
    );
}
```

---

### Teste do Cenário de Falha
1. Simule uma falha no Kafka (desligue o broker).
2. Tente enviar uma mensagem pelo Serviço A.
3. Observe que:
   - O producer tenta reenviar a mensagem (retries).
   - Após esgotar as tentativas, a mensagem é registrada no banco de dados.

### Próximos Passos
Se esta parte fizer sentido, podemos:
1. Adicionar uma rotina para reprocessar mensagens falhadas do banco.
2. Evoluir para a implementação de DLQ e resiliência no consumidor.

Para reprocessar mensagens falhadas do banco, podemos criar uma rotina que seja executada periodicamente ou manualmente, reenvie as mensagens armazenadas, e marque-as como processadas ao serem enviadas com sucesso. 

### Passo 1: Estrutura do Banco de Dados
Certifique-se de ter uma tabela para armazenar mensagens falhadas com uma estrutura básica:

```sql
CREATE TABLE failed_messages (
    id SERIAL PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

- `retry_count`: Para registrar o número de tentativas realizadas.
- `created_at`: Para implementar políticas de expiração, se necessário.

---

### Passo 2: Criar o Serviço de Reprocessamento
Crie um serviço que faça a leitura das mensagens falhadas e tente reenviá-las ao Kafka.

#### Serviço de Reprocessamento
```java
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FailedMessageProcessor {

    private final FailedMessagesRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public FailedMessageProcessor(FailedMessagesRepository repository, KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 10000) // Reprocessa a cada 10 segundos
    @Transactional
    public void reprocessMessages() {
        List<FailedMessage> failedMessages = repository.findTop10(); // Reprocessar em lotes
        for (FailedMessage message : failedMessages) {
            try {
                kafkaTemplate.send(message.getTopic(), message.getMessage()).get(); // Sincrono para garantir envio
                repository.deleteById(message.getId()); // Remove do banco após sucesso
                System.out.println("Message reprocessed successfully: " + message.getMessage());
            } catch (Exception ex) {
                System.err.println("Failed to reprocess message: " + message.getMessage());
                repository.incrementRetryCount(message.getId()); // Incrementa o contador de retries
            }
        }
    }
}
```

---

### Passo 3: Repository para Mensagens Falhadas
Implemente o repositório com métodos para buscar mensagens em lotes e atualizar o contador de retries.

#### Repository
```java
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class FailedMessagesRepository {

    private final JdbcTemplate jdbcTemplate;

    public FailedMessagesRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<FailedMessage> findTop10() {
        return jdbcTemplate.query(
            "SELECT id, topic, message, retry_count FROM failed_messages ORDER BY created_at ASC LIMIT 10",
            (rs, rowNum) -> new FailedMessage(
                rs.getLong("id"),
                rs.getString("topic"),
                rs.getString("message"),
                rs.getInt("retry_count")
            )
        );
    }

    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM failed_messages WHERE id = ?", id);
    }

    public void incrementRetryCount(Long id) {
        jdbcTemplate.update("UPDATE failed_messages SET retry_count = retry_count + 1 WHERE id = ?", id);
    }
}
```

---

### Passo 4: Configuração para Execução Agendada
Adicione a anotação `@EnableScheduling` na classe principal do Spring Boot para habilitar a execução do método agendado.

#### Exemplo
```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ProducerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProducerApplication.class, args);
    }
}
```

---

### Passo 5: Gerenciamento de Retries
1. **Política de Retries**:
   - Limite o número de tentativas, por exemplo, para 3.
   - Após exceder, mova a mensagem para uma tabela de DLQ ou envie para outro tópico.

2. **Política de Expiração**:
   - Exclua mensagens muito antigas (`created_at`) para evitar crescimento descontrolado.

---

### Teste da Rotina
1. Insira manualmente mensagens na tabela `failed_messages` ou cause falhas no envio para popular a tabela.
2. Observe os logs para confirmar o reprocessamento.
3. Verifique se mensagens processadas com sucesso são removidas da tabela.

Esse processo cria uma base robusta para gerenciar mensagens falhadas. Quer evoluir para políticas de retries mais avançadas ou começar a lidar com falhas no consumidor?



Se o banco de dados estiver indisponível, o producer precisará de alternativas para garantir que as mensagens falhadas não sejam perdidas e possam ser reprocessadas posteriormente. Aqui estão algumas estratégias possíveis:

---

### **1. Persistência Local no Filesystem**
O producer pode gravar as mensagens falhadas em um arquivo local no sistema de arquivos, que será utilizado para reprocessamento mais tarde.

#### Exemplo: Persistindo Mensagens em Arquivos Locais
```java
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class LocalFileStorage {

    private static final String FILE_PATH = "failed-messages.txt";

    public void saveFailedMessage(String topic, String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
            writer.write(topic + "::" + message);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Failed to write message to file: " + e.getMessage());
        }
    }

    public List<String> readFailedMessages() {
        try {
            return Files.readAllLines(Paths.get(FILE_PATH));
        } catch (IOException e) {
            System.err.println("Failed to read messages from file: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public void clearFile() {
        try {
            Files.deleteIfExists(Paths.get(FILE_PATH));
        } catch (IOException e) {
            System.err.println("Failed to clear file: " + e.getMessage());
        }
    }
}
```

---

#### Integração com o Producer
Adicione a lógica de fallback no caso de indisponibilidade do banco:

```java
public void sendMessage(String topic, String message) {
    kafkaTemplate.send(topic, message).addCallback(
        result -> System.out.println("Message sent successfully!"),
        ex -> {
            System.err.println("Failed to send message: " + ex.getMessage());
            try {
                failedMessagesRepository.saveFailedMessage(topic, message);
            } catch (Exception dbEx) {
                System.err.println("Failed to save to database, persisting locally...");
                localFileStorage.saveFailedMessage(topic, message);
            }
        }
    );
}
```

---

### **2. Usar Cache Local (In-Memory)**
Se o banco estiver indisponível, o producer pode armazenar mensagens falhadas temporariamente na memória e tentar reprocessá-las periodicamente.

#### Exemplo com `ConcurrentLinkedQueue`
```java
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class InMemoryFailedMessageCache {

    private final Queue<String> failedMessages = new ConcurrentLinkedQueue<>();

    public void saveFailedMessage(String message) {
        failedMessages.add(message);
    }

    public String getFailedMessage() {
        return failedMessages.poll(); // Remove e retorna a próxima mensagem
    }

    public boolean isEmpty() {
        return failedMessages.isEmpty();
    }
}
```

#### Integração com o Producer
```java
public void sendMessage(String topic, String message) {
    kafkaTemplate.send(topic, message).addCallback(
        result -> System.out.println("Message sent successfully!"),
        ex -> {
            System.err.println("Failed to send message: " + ex.getMessage());
            try {
                failedMessagesRepository.saveFailedMessage(topic, message);
            } catch (Exception dbEx) {
                System.err.println("Failed to save to database, caching in memory...");
                inMemoryFailedMessageCache.saveFailedMessage(topic + "::" + message);
            }
        }
    );
}
```

---

### **3. Fallback para Outra Persistência**
Se o banco principal estiver indisponível, configure um banco de dados secundário ou outro armazenamento persistente.

- **Redis** ou **Memcached**: Cache distribuído como fallback para armazenar mensagens temporariamente.
- **NoSQL**: Usar algo como MongoDB ou Cassandra para fallback.

---

### **4. Kafka como Backup (Write to DLQ Topic)**
Se nenhuma opção local for viável, o producer pode enviar as mensagens para um tópico especial no Kafka que serve como "DLQ do Producer".

#### Envio para um Tópico de DLQ
```java
public void sendMessage(String topic, String message) {
    kafkaTemplate.send(topic, message).addCallback(
        result -> System.out.println("Message sent successfully!"),
        ex -> {
            System.err.println("Failed to send message: " + ex.getMessage());
            kafkaTemplate.send("producer-dlq", message);
        }
    );
}
```

- **Benefício**: As mensagens ficam armazenadas no Kafka, e o reprocessamento pode ser feito diretamente de lá, eliminando a necessidade de persistência local.
- **Cuidado**: Garante resiliência, mas aumenta a dependência do Kafka.

---

### **5. Circuit Breaker**
Implemente um **circuit breaker** (usando frameworks como Resilience4j) para pausar a interação com o banco de dados se ele estiver indisponível, enquanto utiliza uma das estratégias acima.

#### Exemplo com Resilience4j
```java
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

import java.time.Duration;

public class CircuitBreakerConfig {

    private final CircuitBreaker circuitBreaker;

    public CircuitBreakerConfig() {
        this.circuitBreaker = CircuitBreaker.of("database", CircuitBreakerConfig.custom()
            .failureRateThreshold(50) // Limite de falhas (em %)
            .waitDurationInOpenState(Duration.ofSeconds(30)) // Tempo de espera para tentar novamente
            .build());
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }
}
```

---

### **Decisão Estratégica**
- **Persistência Local**: Simples e eficaz para sistemas isolados.
- **In-Memory Cache**: Ótimo para sistemas que têm pouca latência e consumo rápido.
- **Fallback para Kafka DLQ**: Ideal para sistemas event-driven que já utilizam Kafka extensivamente.
- **Circuit Breaker**: Garante controle e evita sobrecarregar o banco quando ele está parcialmente indisponível.

Qual dessas abordagens você prefere implementar primeiro?




A anotação `@Retryable` no Spring é uma abordagem específica para implementar a lógica de reprocessamento em caso de falhas. Ela é frequentemente usada no contexto de métodos que podem falhar por razões temporárias e que precisam ser executados novamente, como chamadas a APIs externas, operações de banco de dados, entre outras. A principal diferença entre `@Retryable` e as configurações de retry do Kafka está na forma como elas são aplicadas e no escopo da operação de retry.

### 1. **Retry Configurations no Kafka** (`ProducerConfig.RETRIES_CONFIG`)
As configurações de retry do Kafka, como o `ProducerConfig.RETRIES_CONFIG` e `ProducerConfig.RETRY_BACKOFF_MS_CONFIG`, são específicas para o funcionamento do **producer** do Kafka e controlam como as mensagens são reenviadas para o Kafka quando ocorre uma falha na entrega (geralmente falhas de rede ou problemas temporários com o broker).

- **Escopo**: Configuração de retry no Kafka é **exclusiva para o envio de mensagens**. Ou seja, se o Kafka falhar ao enviar uma mensagem para um tópico, ele tentará reenviar de acordo com as configurações de retry.
- **Controle**: O controle sobre a quantidade de tentativas e o intervalo entre elas é feito diretamente na configuração do Kafka e não no código de aplicação.

### 2. **`@Retryable` do Spring**
A anotação `@Retryable` é uma abstração fornecida pelo Spring que pode ser aplicada a **métodos de qualquer tipo**. Ela oferece a capacidade de definir um comportamento de retry para o método em que é aplicada, permitindo que o Spring reexecute o método automaticamente em caso de exceções específicas. Essa abordagem é mais flexível e pode ser usada em diferentes contextos (não se limita a interações com o Kafka).

#### Exemplo de Uso do `@Retryable`:
```java
@Retryable(value = {IOException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
public void sendMessage() throws IOException {
    // Método que tenta enviar uma mensagem e pode falhar com IOException
    messageSender.send();
}
```

- **Escopo**: A anotação `@Retryable` funciona no nível de **métodos**. Você a aplica diretamente no método que deseja que seja reexecutado em caso de falha. Ela pode ser usada em qualquer tipo de operação (não apenas com Kafka, mas também para interações com APIs externas, chamadas de rede, ou qualquer código suscetível a falhas temporárias).
- **Controle**: A anotação permite especificar **quantas tentativas** serão feitas (`maxAttempts`), o **intervalo de tempo entre tentativas** (`delay`), e outras estratégias de backoff (como backoff exponencial).

### 3. **Principais Diferenças**

- **Escopo e Contexto**:
  - `@Retryable`: Aplica-se a qualquer método da sua aplicação, permitindo retry em operações genéricas.
  - Kafka Retry: Aplica-se apenas ao contexto de envio de mensagens para o Kafka, controlando como o produtor reage a falhas na entrega de mensagens.

- **Configuração**:
  - `@Retryable`: Configurada diretamente no código, como uma anotação sobre o método.
  - Kafka Retry: Configurada através das propriedades do `ProducerConfig` no Kafka.

- **Flexibilidade**:
  - `@Retryable`: Oferece controle sobre os tipos de exceções que disparam a lógica de retry e permite definir parâmetros de retry detalhados como o delay, backoff exponencial, e o número máximo de tentativas.
  - Kafka Retry: Focado nas falhas de comunicação com o Kafka e no reenvio de mensagens; não é tão flexível em termos de controle fino de exceções ou backoff.

### 4. **Quando Usar Cada Uma**

- **Kafka Retry**: Se você está lidando com o envio de mensagens para o Kafka e quer controlar o retry de falhas temporárias (como falhas de rede ou problemas com o broker), utilize as configurações de retry do Kafka. Elas são ideais para garantir a entrega das mensagens e podem ser facilmente configuradas no `ProducerConfig`.

- **`@Retryable`**: Se você precisa de retry em uma lógica de aplicação mais ampla (por exemplo, tentar novamente ao se comunicar com uma API externa ou ao acessar um banco de dados), a anotação `@Retryable` oferece uma maneira fácil e flexível de aplicar o comportamento de retry a métodos específicos, com a possibilidade de personalizar o comportamento para diferentes exceções.

### 5. **Exemplo de Uso Combinado**
Você pode usar tanto o retry do Kafka quanto o `@Retryable` no Spring juntos, se necessário. Por exemplo, você pode configurar retries no Kafka para garantir a entrega das mensagens, enquanto aplica o `@Retryable` em métodos que lidam com chamadas externas, como APIs, dentro da lógica de aplicação.

```java
@Retryable(value = {IOException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
public void callExternalApi() throws IOException {
    // Chamada a API externa que pode falhar temporariamente
    externalApiClient.call();
}
``` 

Essa combinação dá flexibilidade para garantir que você trate falhas de forma robusta tanto nas interações com o Kafka quanto com outros componentes da aplicação.

# V1


Vamos implementar retries no consumidor! A ideia é que, ao processar uma mensagem e encontrar um erro temporário, o sistema tente processar a mensagem novamente antes de considerar a falha definitiva.

### Passo 1: Configurar Retries no Consumidor

#### 1. Adicione Dependências para Suporte a Retries
Certifique-se de que o projeto já tem as dependências do Spring Kafka. Caso contrário, atualize o `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

#### 2. Ajuste as Configurações do Kafka no `application.yml`

```yaml
spring:
  kafka:
    consumer:
      group-id: service-b-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      auto-offset-reset: earliest
    listener:
      ack-mode: record
    template:
      default-topic: example-topic
```

#### 3. Modifique o Consumer para Lidar com Falhas

Adicione um manipulador de erros e configure um `RetryTemplate` para gerenciar as tentativas.

##### Configuração do RetryTemplate

Crie uma classe de configuração para o Kafka:

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class KafkaRetryConfig {

    @Bean
    public DefaultErrorHandler errorHandler() {
        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(3)  // Tentar processar a mensagem até 3 vezes
                .fixedBackoff(1000)  // Intervalo de 1 segundo entre as tentativas
                .build();

        return new DefaultErrorHandler((rec, ex) -> {
            System.err.println("Mensagem movida para DLQ após falhas: " + rec.value());
        }, retryTemplate);
    }
}
```

##### Atualize o Consumer para Usar o ErrorHandler

No consumidor, use o `DefaultErrorHandler` para gerenciar falhas:

```java
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.stereotype.Service;

@Service
public class MessageConsumer {

    private final DefaultErrorHandler errorHandler;

    public MessageConsumer(DefaultErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @KafkaListener(topics = "example-topic", groupId = "service-b-group")
    public void consume(String message) {
        try {
            System.out.println("Processing message: " + message);
            // Simule um erro para testar o retry
            if (message.contains("fail")) {
                throw new RuntimeException("Erro ao processar mensagem");
            }
        } catch (Exception e) {
            errorHandler.handle(null, e, null);
            throw e;  // Rethrow para acionar o retry
        }
    }
}
```

### Passo 2: Testar Retries
1. Envie uma mensagem com o texto `"Hello, Kafka!"` para validar o consumo bem-sucedido.
2. Envie uma mensagem com o texto `"fail"` para simular falhas e verificar as tentativas de reprocessamento.
3. Confirme que o sistema tenta reprocessar a mensagem 3 vezes antes de falhar definitivamente.

Queremos incluir um DLQ para armazenar mensagens não processadas após as tentativas ou ajustar algum aspecto dessa implementação?



Adicionar uma **Dead Letter Queue (DLQ)** é uma ótima maneira de lidar com mensagens que não podem ser processadas após várias tentativas. A DLQ armazenará essas mensagens para análise posterior, garantindo que nenhuma mensagem seja perdida.

### Passo 1: Configurar a DLQ no Kafka
1. Crie um tópico dedicado para a DLQ:
   - Nome sugerido: `example-topic-dlq`.
   - Use ferramentas como `kafka-topics.sh` ou um painel de administração do Kafka para criar o tópico.

### Passo 2: Ajustar o Consumer para Usar a DLQ

#### 1. Atualizar Configuração no `application.yml`
Adicione a configuração da DLQ no consumidor:

```yaml
spring:
  kafka:
    consumer:
      group-id: service-b-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      auto-offset-reset: earliest
    listener:
      ack-mode: record
    template:
      default-topic: example-topic
    dead-letter-topic: example-topic-dlq
```

#### 2. Atualizar o `DefaultErrorHandler`
Modifique a configuração para enviar mensagens para o tópico DLQ após falhas definitivas.

```java
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class KafkaRetryConfig {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaRetryConfig(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Bean
    public DefaultErrorHandler errorHandler() {
        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(3)
                .fixedBackoff(1000)
                .build();

        return new DefaultErrorHandler((ConsumerRecord<?, ?> record, Exception ex) -> {
            System.err.println("Falha definitiva. Movendo mensagem para DLQ: " + record.value());
            kafkaTemplate.send(new ProducerRecord<>(
                    "example-topic-dlq",
                    null,
                    record.key() == null ? null : record.key().toString(),
                    record.value().toString(),
                    record.headers()
            ));
        }, retryTemplate);
    }
}
```

#### 3. Consumir Mensagens da DLQ (Opcional)
Se você quiser processar ou analisar as mensagens da DLQ, configure outro consumidor:

```java
@Service
public class DlqConsumer {

    @KafkaListener(topics = "example-topic-dlq", groupId = "dlq-group")
    public void consumeDlq(String message) {
        System.out.println("Mensagem da DLQ: " + message);
        // Aqui você pode salvar em um banco de dados ou enviar alertas.
    }
}
```

### Passo 3: Testar a DLQ
1. **Cenário Feliz**: Envie uma mensagem como `"Hello, Kafka!"` e confirme o processamento bem-sucedido.
2. **Cenário de Falha**: Envie uma mensagem como `"fail"`. A mensagem deve falhar 3 vezes (retry) e, em seguida, ser movida para o tópico `example-topic-dlq`.
3. **Verificar DLQ**: Verifique se a mensagem aparece no consumidor da DLQ ou use ferramentas de administração do Kafka para visualizá-la.

### Próximo Passo
Se desejar, podemos:
- Melhorar o fluxo adicionando metadados à mensagem antes de movê-la para a DLQ.
- Implementar alertas automáticos para mensagens na DLQ.
- Passar para outra técnica de resiliência, como **idempotência** ou **persistência local no producer**. 

Qual é o próximo passo? 😊




A **persistência local no Producer** é uma boa prática para garantir que as mensagens sejam armazenadas temporariamente antes de serem enviadas ao Kafka, caso ocorra uma falha durante o envio. Isso pode ser especialmente útil em situações em que o Kafka está temporariamente inacessível ou quando o producer precisa ser reiniciado. Ao armazenar as mensagens localmente, você pode garantir que elas não sejam perdidas.

Aqui estão os passos para implementar essa persistência:

---

### Passo 1: Armazenamento Local (Persistência em Arquivo ou Banco de Dados)

Para persistir localmente, você pode usar uma abordagem simples, como escrever as mensagens em um arquivo (por exemplo, no formato JSON ou texto). Outra alternativa seria persistir em um banco de dados, mas vamos começar com uma solução mais simples usando arquivos.

#### 1. Criação de um Serviço de Persistência

Vamos criar um serviço para persistir as mensagens em um arquivo antes de enviá-las ao Kafka.

```java
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import org.springframework.stereotype.Service;

@Service
public class LocalPersistenceService {

    private static final String FILE_PATH = "messages-to-send.txt";

    public void persistMessage(String key, String value) {
        try (FileWriter fileWriter = new FileWriter(FILE_PATH, true);
             PrintWriter printWriter = new PrintWriter(fileWriter)) {
            printWriter.printf("Key: %s, Message: %s%n", key, value);
        } catch (IOException e) {
            System.err.println("Erro ao persistir a mensagem: " + e.getMessage());
        }
    }

    public boolean hasMessagesToSend() {
        // Método para verificar se há mensagens armazenadas para reenvio
        return new java.io.File(FILE_PATH).exists();
    }
}
```

#### 2. Modificar o Producer para Usar a Persistência Local

Agora, vamos modificar o `MessageProducer` para usar o `LocalPersistenceService` e garantir que as mensagens sejam armazenadas antes de serem enviadas ao Kafka.

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessageProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final LocalPersistenceService persistenceService;

    @Autowired
    public MessageProducer(KafkaTemplate<String, String> kafkaTemplate, LocalPersistenceService persistenceService) {
        this.kafkaTemplate = kafkaTemplate;
        this.persistenceService = persistenceService;
    }

    public void sendMessage(String key, String value) {
        // Persistir a mensagem localmente antes de enviar ao Kafka
        persistenceService.persistMessage(key, value);

        kafkaTemplate.send("example-topic", key, value)
            .addCallback(
                result -> System.out.println("Mensagem enviada com sucesso: " + value),
                ex -> {
                    System.err.println("Erro ao enviar mensagem: " + ex.getMessage());
                    // Caso o envio falhe, a mensagem já está persistida e pode ser reenviada depois
                }
            );
    }
}
```

### Passo 2: Implementar Reenvio de Mensagens Persistidas

Caso a aplicação falhe ou o Kafka esteja temporariamente inacessível, podemos implementar um mecanismo para reprocessar as mensagens armazenadas localmente. Vamos criar um método para tentar enviar as mensagens armazenadas.

#### 1. Criar um Serviço de Reenvio

```java
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageResendService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final LocalPersistenceService persistenceService;

    @Autowired
    public MessageResendService(KafkaTemplate<String, String> kafkaTemplate, LocalPersistenceService persistenceService) {
        this.kafkaTemplate = kafkaTemplate;
        this.persistenceService = persistenceService;
    }

    public void resendMessages() {
        // Verifique se há mensagens persistidas
        if (persistenceService.hasMessagesToSend()) {
            try {
                List<String> messages = Files.readAllLines(Paths.get("messages-to-send.txt"))
                    .stream()
                    .collect(Collectors.toList());

                for (String message : messages) {
                    // Enviar as mensagens para o Kafka
                    String[] parts = message.split(", ");
                    String key = parts[0].split(": ")[1];
                    String value = parts[1].split(": ")[1];

                    kafkaTemplate.send("example-topic", key, value)
                        .addCallback(
                            result -> System.out.println("Mensagem reenviada com sucesso: " + value),
                            ex -> System.err.println("Erro ao reenviar mensagem: " + ex.getMessage())
                        );
                }
            } catch (Exception e) {
                System.err.println("Erro ao processar mensagens para reenvio: " + e.getMessage());
            }
        }
    }
}
```

#### 2. Programar o Reenvio (Caso a Aplicação Reinicie)
Você pode configurar um agendador (por exemplo, com Spring Scheduler) para tentar reenviar as mensagens persistidas periodicamente, caso o Kafka esteja acessível.

```java
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledMessageResend {

    private final MessageResendService resendService;

    public ScheduledMessageResend(MessageResendService resendService) {
        this.resendService = resendService;
    }

    @Scheduled(fixedRate = 5000)  // Tentar reenviar a cada 5 segundos
    public void resendPersistedMessages() {
        resendService.resendMessages();
    }
}
```

### Passo 3: Testar a Persistência Local
1. **Enviar Mensagens**: Envie algumas mensagens usando o `MessageProducer`.
2. **Simular Falha de Envio**: Pare o Kafka ou force uma falha de rede para ver se as mensagens são persistidas no arquivo.
3. **Reiniciar a Aplicação**: Após reiniciar, verifique se as mensagens persistidas são reenviadas para o Kafka.

### Passo 4: Melhorias e Alternativas

- **Persistência com Banco de Dados**: Em vez de usar arquivos, você pode utilizar um banco de dados para armazenar mensagens persistidas. Isso pode ser mais eficiente e escalável.
- **Persistência em uma Fila Local (como um Banco de Mensagens Local)**: Para maior resiliência, pode-se até usar uma fila local como o RabbitMQ ou uma fila em memória, como o Apache Pulsar.
- **Monitoramento e Alertas**: Monitore as falhas de envio e reenvios para enviar alertas ou notificar os administradores.

Agora que a persistência local foi implementada, temos uma solução robusta para garantir que mensagens não sejam perdidas. Quer avançar para outra área, como melhorar o controle de falhas, ou algo mais? 😊




Implementar lógica de **retry** e **fallback** para operações sensíveis, como o envio de mensagens ao Kafka, é essencial para tornar o sistema resiliente a falhas temporárias ou intermitentes. Vou detalhar como fazer isso e as melhores práticas:

---

### **1. O Que é Retry?**
**Retry** é a tentativa de repetir uma operação após uma falha, com a esperança de que o problema seja temporário e a operação tenha sucesso em uma nova tentativa.

#### Exemplos de cenários onde retry faz sentido:
- O Kafka está temporariamente indisponível (problemas de conectividade).
- Falhas transitórias no envio (e.g., timeouts).

#### **Implementando Retry com KafkaTemplate**
O `KafkaTemplate` não tem retry nativo, mas você pode implementar manualmente utilizando um bloco `try-catch` e um mecanismo de repetição controlada.

##### Exemplo básico de retry:
```java
public void enviarComRetry(String topic, String key, Object value) {
    int maxRetries = 3; // Número máximo de tentativas
    int attempt = 0;
    boolean enviado = false;

    while (attempt < maxRetries && !enviado) {
        try {
            kafkaTemplate.send(topic, key, value).get(); // Confirma envio
            log.info("Mensagem enviada com sucesso na tentativa {}", attempt + 1);
            enviado = true; // Saída do loop
        } catch (Exception e) {
            attempt++;
            log.error("Erro ao enviar mensagem para Kafka. Tentativa {}/{}", attempt, maxRetries, e);
            if (attempt == maxRetries) {
                throw new RuntimeException("Falha ao enviar mensagem para Kafka após várias tentativas", e);
            }
        }
    }
}
```

---

### **2. Backoff Estratégico**
Repetir uma operação imediatamente pode sobrecarregar o sistema e não resolver o problema. É importante adicionar um atraso entre as tentativas, conhecido como **backoff**.

#### Tipos de Backoff:
1. **Fixo**: Um atraso constante entre tentativas.
2. **Exponencial**: Aumenta o atraso a cada falha (e.g., 100ms, 200ms, 400ms).
3. **Jitter**: Adiciona um valor aleatório ao backoff exponencial para evitar picos simultâneos.

##### Exemplo com backoff fixo:
```java
public void enviarComRetryBackoff(String topic, String key, Object value) {
    int maxRetries = 3;
    int attempt = 0;
    int backoffMillis = 1000; // 1 segundo

    while (attempt < maxRetries) {
        try {
            kafkaTemplate.send(topic, key, value).get();
            log.info("Mensagem enviada com sucesso na tentativa {}", attempt + 1);
            return; // Sucesso
        } catch (Exception e) {
            attempt++;
            log.error("Erro ao enviar mensagem. Tentativa {}/{}", attempt, maxRetries, e);
            if (attempt == maxRetries) {
                throw new RuntimeException("Falha ao enviar mensagem após múltiplas tentativas", e);
            }
            try {
                Thread.sleep(backoffMillis); // Aguardar antes da próxima tentativa
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Retry interrompido", ie);
            }
        }
    }
}
```

---

### **3. O Que é Fallback?**
**Fallback** é a execução de uma ação alternativa ou de compensação caso todas as tentativas falhem.

#### Exemplos de fallback:
- Persistir a mensagem em um banco de dados ou uma fila local para reprocessamento posterior.
- Enviar uma notificação ou alerta para o time responsável.

##### Exemplo de fallback:
```java
public void enviarMensagemComFallback(String topic, String key, Object value) {
    int maxRetries = 3;
    try {
        enviarComRetry(topic, key, value); // Tentativa com retry
    } catch (Exception e) {
        log.error("Falha ao enviar mensagem após retries. Executando fallback.", e);
        salvarParaReprocessamento(value); // Persistir para reprocessamento posterior
    }
}

private void salvarParaReprocessamento(Object value) {
    try {
        log.info("Salvando mensagem para reprocessamento: {}", value);
        // Exemplo: salvar em um banco de dados ou em uma fila local
        fallbackRepository.save(value);
    } catch (Exception e) {
        log.error("Falha ao salvar mensagem no fallback storage", e);
    }
}
```

---

### **4. Integração com Bibliotecas de Resiliência**
Além de implementar manualmente, você pode usar bibliotecas que abstraem essa lógica de forma elegante, como o **Resilience4j**.

#### Exemplo com Resilience4j:
```java
RetryConfig config = RetryConfig.custom()
    .maxAttempts(3)
    .waitDuration(Duration.ofSeconds(1)) // Backoff fixo
    .build();

Retry retry = Retry.of("kafkaProducerRetry", config);

public void enviarMensagem(String topic, String key, Object value) {
    Retry.decorateCheckedRunnable(retry, () -> kafkaTemplate.send(topic, key, value).get()).run();
}
```

---

### **5. Cuidados ao Implementar Retry**
- **Evitar Efeitos Colaterais**: As operações devem ser idempotentes para evitar impactos duplicados (e.g., múltiplos registros no banco ou mensagens duplicadas).
- **Monitorar Erros**: Use logs ou métricas para acompanhar falhas e retries.
- **Evitar Retry Infinito**: Sempre defina um número máximo de tentativas.

---

### Resumo
Implementar **retry** e **fallback** ajuda a garantir a resiliência do sistema ao lidar com falhas. A estratégia deve ser cuidadosamente configurada para evitar sobrecarga e impactos negativos. Se precisar, podemos explorar em mais detalhes ou integrar essas técnicas em um exemplo mais completo!



Usar o **Spring Kafka** facilita bastante a implementação de lógicas de retry e fallback, pois a biblioteca já oferece suporte nativo para várias dessas funcionalidades, tanto no produtor quanto no consumidor. Abaixo, adapto os exemplos usando Spring Kafka.

---

## **1. Retry no Producer com Spring Kafka**

No caso do **Producer**, você pode usar as funcionalidades de retry oferecidas pelo Spring Retry, integrando-o ao envio de mensagens via `KafkaTemplate`.

### Configuração do Retry com Backoff
Adicione uma configuração de retry com backoff usando o `RetryTemplate` do Spring Retry.

```java
@Configuration
public class KafkaProducerConfig {

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory, producerRetryTemplate());
    }

    private RetryTemplate producerRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Configurar política de retry
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(1000); // 1 segundo entre tentativas
        retryTemplate.setBackOffPolicy(backOffPolicy);

        // Configurar número máximo de tentativas
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3); // 3 tentativas
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }
}
```

### Enviando Mensagens com Retry
Use o `KafkaTemplate` normalmente; o retry será gerenciado automaticamente.

```java
@Service
public class PixProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PixProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void enviarMensagem(String topic, String key, Object value) {
        kafkaTemplate.send(topic, key, value).addCallback(
            success -> log.info("Mensagem enviada com sucesso: {}", success.getProducerRecord()),
            failure -> log.error("Erro ao enviar mensagem para o Kafka", failure)
        );
    }
}
```

---

## **2. Retry no Consumer com Spring Kafka**

O Spring Kafka suporta retry nativo no consumidor com o uso de `SeekToCurrentErrorHandler` ou outros `ErrorHandler`.

### Configuração do Retry no Consumer
Crie um `ConcurrentKafkaListenerContainerFactory` e configure o retry com backoff.

```java
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        // Configuração de retry com backoff
        factory.setErrorHandler(
                new SeekToCurrentErrorHandler(new DeadLetterPublishingRecoverer(kafkaTemplate()), 
                                              new FixedBackOff(1000, 3)) // 1s, 3 tentativas
        );

        return factory;
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return props;
    }
}
```

---

## **3. Fallback para DLQ no Consumer**

A configuração acima inclui um **DeadLetterPublishingRecoverer** para redirecionar mensagens falhas a uma Dead Letter Queue (DLQ).

### Criando um Listener para DLQ
O DLQ é configurado automaticamente pelo Spring Kafka. Você pode criar um listener para monitorar mensagens enviadas para a fila de erro.

```java
@Component
public class DlqListener {

    @KafkaListener(topics = "pix-topic-dlq")
    public void processarMensagemDLQ(ConsumerRecord<String, Object> record) {
        log.error("Mensagem enviada para DLQ: {}", record);
        // Processar ou notificar time responsável
    }
}
```

---

## **4. Garantia de Idempotência no Consumer**

Para garantir idempotência no consumidor, você pode usar um identificador único (`key`) da mensagem e armazenar um registro de processamentos já realizados, evitando duplicação.

### Exemplo com Cache
```java
@Service
public class PixConsumer {

    private final Set<String> processedKeys = ConcurrentHashMap.newKeySet();

    @KafkaListener(topics = "pix-topic", groupId = "pix-group")
    public void consumirMensagem(ConsumerRecord<String, PixDTO> record) {
        String key = record.key();
        if (processedKeys.contains(key)) {
            log.info("Mensagem já processada: {}", key);
            return; // Ignorar mensagem duplicada
        }

        try {
            log.info("Processando mensagem: {}", record);
            processedKeys.add(key);
            // Processar lógica do Pix
        } catch (Exception e) {
            log.error("Erro ao processar mensagem", e);
            throw e; // Deixar o retry/dlq tratar a falha
        }
    }
}
```

---

## Resumo

Essas implementações são práticas e alinhadas ao ecossistema do Spring Kafka. O uso de `RetryTemplate`, `SeekToCurrentErrorHandler`, e `DeadLetterPublishingRecoverer` fornece mecanismos robustos para retry e fallback. Sempre monitore suas métricas e ajuste os parâmetros de retry e DLQ de acordo com as necessidades do sistema.

Se precisar de algum ajuste ou de mais explicações, é só pedir!









A estrutura de pacotes para aplicações que utilizam **Scheduler** no Spring Boot deve ser organizada para refletir os princípios de modularidade e responsabilidade única. Uma boa prática é separar a lógica de agendamento da lógica de negócios e manter a configuração desacoplada. Aqui está uma sugestão de estrutura de pacotes que você pode usar:

---

### **1. Estrutura Sugerida**

```plaintext
br.com.raysonsatos.producer_a
├── config               # Configurações e Beans da aplicação
│   └── KafkaProducerConfig.java
│
├── domain               # Entidades ou objetos de domínio (opcional, depende da aplicação)
│   └── Pix.java
│
├── dto                  # Objetos de transferência de dados (Data Transfer Objects)
│   └── PixDTO.java
│
├── service              # Serviços de negócio (lógica principal)
│   └── ProducerService.java
│
├── scheduler            # Componentes responsáveis por tarefas agendadas
│   └── MessageScheduler.java
│
├── repository           # Interfaces para persistência de dados
│   └── PixRepository.java
│
├── controller           # Controladores REST (API) se necessário
│   └── PixController.java
│
├── util                 # Classes utilitárias e helpers (opcional)
│   └── KafkaUtils.java
│
└── ProducerAApplication.java  # Classe principal da aplicação
```

---

### **2. Justificativa da Estrutura**

1. **`config`**:
   - Para todas as configurações da aplicação, como `KafkaProducerConfig`, configurações do Scheduler, e outros Beans.
   - Mantém as configurações centralizadas e facilita a manutenção.

2. **`domain`**:
   - Armazena as entidades da aplicação, como classes representando objetos de domínio (e.g., `Pix.java`).
   - Separar o domínio ajuda a seguir o conceito de Domain-Driven Design.

3. **`dto`**:
   - Armazena classes utilizadas para transferir dados entre camadas, como o `PixDTO`.
   - Garante que o modelo de domínio não seja exposto diretamente.

4. **`service`**:
   - Contém a lógica de negócios, como a geração de mensagens para o Kafka.
   - Facilita o teste e reutilização da lógica central.

5. **`scheduler`**:
   - Contém todas as classes relacionadas ao agendamento de tarefas.
   - Separar agendadores permite que alterações na periodicidade ou lógica de agendamento não impactem outras partes da aplicação.

6. **`repository`**:
   - Para persistência de dados, normalmente com Spring Data JPA ou outro framework de ORM.
   - Centraliza o acesso a dados.

7. **`controller`**:
   - Para expor endpoints REST, se necessário, facilitando a comunicação com outros sistemas ou usuários.

8. **`util`**:
   - Para classes que encapsulam funcionalidades genéricas, como métodos auxiliares para formatação ou validação.

---

### **3. Exemplo Específico: Aplicação com Scheduler**

Se a aplicação for simples e não envolver persistência ou uma API REST, você pode omitir os pacotes `domain`, `repository`, e `controller`. A estrutura ficaria assim:

```plaintext
br.com.raysonsatos.producer_a
├── config               # Configurações do Kafka e Scheduler
│   └── KafkaProducerConfig.java
│
├── service              # Lógica de envio de mensagens
│   └── ProducerService.java
│
├── scheduler            # Tarefas agendadas
│   └── MessageScheduler.java
│
└── ProducerAApplication.java  # Classe principal
```

---

Essa estrutura é flexível e pode ser adaptada conforme as necessidades do projeto. Separar as responsabilidades facilita a escalabilidade, testes e manutenção do código.









Para configurar seu projeto Spring Boot com **Spring Cloud AWS** e **AWS Secrets Manager** para utilizar perfis diferentes (local, dev, hml e prod) com base na máquina de execução (local ou AWS), você pode seguir as etapas abaixo.

A ideia é usar o `application.yml` ou `application.properties` para configurar diferentes perfis e carregar os segredos do **AWS Secrets Manager** dependendo do ambiente. Vamos configurar isso da seguinte forma:

### **1. Estrutura do `application.yml` para Perfis Diferentes**

Aqui está um exemplo de como organizar as configurações no `application.yml`, dividindo os perfis `local`, `dev`, `hml` e `prod`.

```yaml
# application.yml

spring:
  cloud:
    aws:
      region:
        static: us-east-1  # Defina a região AWS padrão
      secrets-manager:
        enabled: true  # Habilitar o uso do Secrets Manager

---
# Perfil Local
spring:
  profiles: local
  cloud:
    aws:
      secrets-manager:
        name: your-secrets-manager-local  # Nome do segredo para o perfil local
        fail-fast: false  # Não falhar se não encontrar o segredo (útil para desenvolvimento local)
  kafka:
    bootstrap-servers: localhost:9092  # Configuração para o Kafka local
    properties:
      security.protocol: PLAINTEXT
    consumer:
      group-id: local-group

---
# Perfil Dev
spring:
  profiles: dev
  cloud:
    aws:
      secrets-manager:
        name: your-secrets-manager-dev  # Nome do segredo para o perfil de dev
        fail-fast: true  # Falha caso não consiga recuperar o segredo
  kafka:
    bootstrap-servers: your-kafka-dev-broker-url:9092  # URL do Kafka na AWS (ex: MSK)
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: PLAIN
      sasl.jaas.config: >
        org.apache.kafka.common.security.plain.PlainLoginModule required
        username="${kafka.username}"
        password="${kafka.password}";

---
# Perfil HML
spring:
  profiles: hml
  cloud:
    aws:
      secrets-manager:
        name: your-secrets-manager-hml  # Nome do segredo para o perfil de homologação
        fail-fast: true
  kafka:
    bootstrap-servers: your-kafka-hml-broker-url:9092  # URL do Kafka para homologação
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: PLAIN
      sasl.jaas.config: >
        org.apache.kafka.common.security.plain.PlainLoginModule required
        username="${kafka.username}"
        password="${kafka.password}";

---
# Perfil Prod
spring:
  profiles: prod
  cloud:
    aws:
      secrets-manager:
        name: your-secrets-manager-prod  # Nome do segredo para o perfil de produção
        fail-fast: true
  kafka:
    bootstrap-servers: your-kafka-prod-broker-url:9092  # URL do Kafka em produção
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: PLAIN
      sasl.jaas.config: >
        org.apache.kafka.common.security.plain.PlainLoginModule required
        username="${kafka.username}"
        password="${kafka.password}";
```

### **2. Configuração de Perfis no AWS Secrets Manager**

Cada perfil (local, dev, hml, prod) deve ter um segredo no **AWS Secrets Manager** com informações específicas de cada ambiente. O nome do segredo será passado no parâmetro `name`, conforme configurado no `application.yml`.

Exemplo de formato do segredo para cada ambiente:

- **Local:**
```json
{
  "kafka.username": "local_user",
  "kafka.password": "local_password"
}
```

- **Dev:**
```json
{
  "kafka.username": "dev_user",
  "kafka.password": "dev_password"
}
```

- **HML:**
```json
{
  "kafka.username": "hml_user",
  "kafka.password": "hml_password"
}
```

- **Prod:**
```json
{
  "kafka.username": "prod_user",
  "kafka.password": "prod_password"
}
```

### **3. Como Passar o Perfil para a Aplicação**

Você pode passar o perfil ativo para o Spring Boot ao iniciar a aplicação, de duas formas:

1. **Via linha de comando** ao iniciar a aplicação:

```bash
java -jar your-app.jar --spring.profiles.active=dev
```

Ou para o perfil de produção:

```bash
java -jar your-app.jar --spring.profiles.active=prod
```

2. **Via variável de ambiente**:

No seu ambiente AWS (como EC2 ou ECS), você pode definir a variável de ambiente `SPRING_PROFILES_ACTIVE` com o valor desejado:

```bash
export SPRING_PROFILES_ACTIVE=dev
```

### **4. Configuração para Local (Desenvolvimento)**
Para o perfil `local`, você pode definir configurações alternativas, como o Kafka local ou o uso de um banco de dados em memória. Por exemplo:

```yaml
# application-local.yml
spring:
  kafka:
    bootstrap-servers: localhost:9092  # Kafka rodando localmente
```

### **5. Configuração no AWS**

Quando a aplicação estiver rodando na AWS, o Spring Cloud AWS automaticamente irá buscar os segredos no **AWS Secrets Manager** de acordo com o nome definido em cada perfil (`your-secrets-manager-dev`, `your-secrets-manager-prod`, etc.). Assim, as credenciais do Kafka e outras informações sensíveis serão recuperadas de forma automática.

Certifique-se de que a instância EC2 ou o container ECS tenha permissões adequadas via **IAM role** para acessar o **AWS Secrets Manager**.

### **6. Testando Localmente**

Ao testar localmente, você pode usar o perfil `local` e criar um arquivo `application-local.yml` contendo configurações específicas, como um Kafka local e configurações para desenvolvimento. Isso permite que sua aplicação tenha uma configuração diferente para ambientes locais e para os ambientes AWS (dev, hml, prod).

### **Conclusão**

Com a configuração acima, você terá perfis separados para `local`, `dev`, `hml` e `prod`, permitindo um controle refinado das configurações em cada ambiente. O Spring Cloud AWS cuida da recuperação dos segredos automaticamente, sem a necessidade de manipulação explícita de credenciais no código ou no `application.yml`. Isso melhora a segurança e facilita a adaptação para diferentes ambientes de execução.






Para que os segredos armazenados no **AWS Secrets Manager** sejam automaticamente convertidos em variáveis de ambiente na sua aplicação, você pode usar um processo automático durante a inicialização da sua aplicação, onde você recupera os segredos do AWS Secrets Manager e os define como variáveis de ambiente. Isso pode ser feito de maneira prática com a utilização de um **AWS Lambda** ou **um script customizado** que configura as variáveis de ambiente ao iniciar a aplicação.

### **Passos para Transformar Segredos em Variáveis de Ambiente Automaticamente**

#### **1. Recuperando Segredos no AWS Secrets Manager**
Você já configurou o seu segredo no **AWS Secrets Manager**. O segredo será um objeto JSON com os campos de credenciais, como `username` e `password`.

Exemplo do segredo:
```json
{
  "username": "your_username",
  "password": "your_password"
}
```

#### **2. Carregando Segredos e Configurando Variáveis de Ambiente**

Para fazer isso de maneira automática, você pode usar um **script de inicialização** no **Docker** ou em um script de inicialização no ambiente de execução (por exemplo, no EC2 ou ECS).

Aqui está um exemplo de como fazer isso em uma aplicação Spring Boot. O código abaixo recupera os segredos e define como variáveis de ambiente.

##### **Exemplo em Java com Spring Boot**

1. **Dependências**

Adicione as dependências necessárias para o SDK da AWS no seu `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>secretsmanager</artifactId>
        <version>2.x.x</version>
    </dependency>
</dependencies>
```

2. **Script para Carregar Segredos e Definir Variáveis de Ambiente**

Aqui está o código Java que recupera os segredos do AWS Secrets Manager e define as variáveis de ambiente:

```java
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SecretsLoader {

    public static void main(String[] args) {
        // Configurar o cliente do Secrets Manager
        SecretsManagerClient client = SecretsManagerClient.builder()
                .region(Region.US_EAST_1) // Substitua pela sua região
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        // Recuperar o segredo
        GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId("kafka-sasl-credentials") // Nome do segredo
                .build();

        GetSecretValueResponse response = client.getSecretValue(request);
        ObjectMapper mapper = new ObjectMapper();

        try {
            // Parse o JSON do segredo
            Map<String, String> credentials = mapper.readValue(response.secretString(), Map.class);
            // Definir como variáveis de ambiente
            System.setProperty("KAFKA_USERNAME", credentials.get("username"));
            System.setProperty("KAFKA_PASSWORD", credentials.get("password"));

            // Imprimir para verificar se as variáveis foram configuradas corretamente
            System.out.println("KAFKA_USERNAME: " + System.getProperty("KAFKA_USERNAME"));
            System.out.println("KAFKA_PASSWORD: " + System.getProperty("KAFKA_PASSWORD"));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar segredos do Secrets Manager", e);
        }
    }
}
```

3. **Configurando no `application.yml`**

No seu `application.yml`, você pode referenciar essas variáveis de ambiente como segue:

```yaml
spring:
  kafka:
    bootstrap-servers: your-broker-url:9092
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: PLAIN
      sasl.jaas.config: >
        org.apache.kafka.common.security.plain.PlainLoginModule required
        username="${KAFKA_USERNAME}"
        password="${KAFKA_PASSWORD}";
```

#### **3. Alternativa com Script de Inicialização no Docker**

Se você estiver usando **Docker**, pode incluir um script de inicialização que execute esse código Java para configurar as variáveis de ambiente antes de rodar sua aplicação Spring Boot.

Aqui está um exemplo de como configurar isso em um Dockerfile:

1. **Dockerfile**

```dockerfile
FROM openjdk:11-jre-slim

WORKDIR /app

# Copie os arquivos do projeto
COPY target/your-application.jar /app/

# Instale o AWS CLI para acessar o Secrets Manager
RUN apt-get update && apt-get install -y awscli

# Script de inicialização
COPY init-secrets.sh /app/

# Execute o script de inicialização e, em seguida, a aplicação
CMD ["sh", "init-secrets.sh"]
```

2. **init-secrets.sh**

```bash
#!/bin/bash

# Recupera as credenciais do AWS Secrets Manager
KAFKA_CREDS=$(aws secretsmanager get-secret-value --secret-id kafka-sasl-credentials --query SecretString --output text)

# Extrai os valores de username e password
KAFKA_USERNAME=$(echo $KAFKA_CREDS | jq -r '.username')
KAFKA_PASSWORD=$(echo $KAFKA_CREDS | jq -r '.password')

# Exporta como variáveis de ambiente
export KAFKA_USERNAME
export KAFKA_PASSWORD

# Executa a aplicação Spring Boot
java -jar /app/your-application.jar
```

O script usa o **AWS CLI** para acessar os segredos e o **jq** para parsear o JSON. Ele define as variáveis de ambiente antes de iniciar a aplicação Spring Boot.

#### **4. Configuração no ECS ou EC2**

Se você estiver usando o **Amazon ECS** ou **EC2**, pode configurar um script de inicialização similar ao exemplo acima, ou usar o **AWS Systems Manager Parameter Store** ou **Secrets Manager** diretamente para passar as variáveis de ambiente para o contêiner ou instância.

---

### **Conclusão**

A forma de configurar as variáveis de ambiente automaticamente a partir dos segredos do AWS Secrets Manager depende do seu ambiente de execução:

- **Para aplicações locais ou em contêineres (Docker),** você pode usar o código Java para recuperar os segredos e definir as variáveis de ambiente diretamente.
- **Para ambientes como ECS ou EC2,** você pode utilizar o AWS CLI ou o SDK para configurar as variáveis antes de iniciar a aplicação.

Essa abordagem garante que você possa manter as credenciais seguras e fora do código, seguindo o princípio do **12-factor app**.








# Aqui

Se você deseja usar **AWS Secrets Manager** para armazenar de forma segura o **usuário** e a **senha** para autenticação no Kafka com SASL/PLAIN, aqui está o processo:

---

### **1. Configurando o Segredo no AWS Secrets Manager**

No AWS Secrets Manager, crie um segredo para armazenar as credenciais. Por exemplo:

- **Secret Name:** `kafka-sasl-credentials`
- **Secret Value (JSON):**
  ```json
  {
    "username": "your_username",
    "password": "your_password"
  }
  ```

---

### **2. Adicionando Dependências**

No seu `pom.xml`, inclua a dependência para integrar com o **AWS SDK** para Java:

```xml
<dependencies>
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>secretsmanager</artifactId>
        <version>2.20.35</version> <!-- Use a versão mais recente disponível -->
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
</dependencies>
```

---

### **3. Recuperando o Segredo no Código**

Crie um componente para buscar as credenciais do Secrets Manager:

```java
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
public class SecretsManagerUtil {

    private final SecretsManagerClient secretsManagerClient;

    public SecretsManagerUtil() {
        this.secretsManagerClient = SecretsManagerClient.builder()
                .region(Region.US_EAST_1) // Substitua pela sua região
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    public Map<String, String> getKafkaCredentials(String secretName) {
        GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();

        GetSecretValueResponse response = secretsManagerClient.getSecretValue(request);

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(response.secretString(), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter segredo do Secrets Manager", e);
        }
    }
}
```

---

### **4. Configurando SASL/PLAIN Dinamicamente**

Use as credenciais recuperadas para configurar o Kafka dinamicamente:

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    private final SecretsManagerUtil secretsManagerUtil;

    public KafkaProducerConfig(SecretsManagerUtil secretsManagerUtil) {
        this.secretsManagerUtil = secretsManagerUtil;
    }

    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();

        // Recupera as credenciais do AWS Secrets Manager
        Map<String, String> credentials = secretsManagerUtil.getKafkaCredentials("kafka-sasl-credentials");

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "your-broker-url:9092");
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        props.put(SaslConfigs.SASL_JAAS_CONFIG, String.format(
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                credentials.get("username"), credentials.get("password")
        ));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        return props;
    }
}
```

---

### **5. Configurando `application.yml`**
Configure apenas as propriedades básicas no `application.yml`:

```yaml
spring:
  kafka:
    producer:
      properties:
        bootstrap.servers: your-broker-url:9092
```

---

### **6. Segurança em Produção**

- **Permissões IAM:** Certifique-se de que a aplicação tem permissões específicas para acessar o Secrets Manager.
  - Exemplo de política IAM:
    ```json
    {
      "Version": "2012-10-17",
      "Statement": [
        {
          "Effect": "Allow",
          "Action": "secretsmanager:GetSecretValue",
          "Resource": "arn:aws:secretsmanager:your-region:your-account-id:secret:kafka-sasl-credentials"
        }
      ]
    }
    ```

- **Rotação de Segredos:** O AWS Secrets Manager permite a rotação automática de segredos, garantindo que as credenciais sejam atualizadas regularmente sem intervenção manual.

---

### Benefícios
- Segregação de credenciais sensíveis do código-fonte.
- Compatibilidade com o **12-factor app**, externalizando configurações.
- Fácil integração com outros serviços AWS.

Essa abordagem fornece uma solução segura e escalável para gerenciar credenciais Kafka em produção.