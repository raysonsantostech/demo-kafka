# alura-kafka

## Criação do Projeto

```bash
mvn archetype:generate -DgroupId=dev.raysons -DartifactId=ecommerce -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false

mvn archetype:generate -DgroupId=dev.raysons -DartifactId=service-email  -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false


```

```xml
  <properties>
      <maven.compiler.source>21</maven.compiler.source>
      <maven.compiler.target>21</maven.compiler.target>
  </properties>
```

## Perguntas?
- Como seria a autenticação no tópico?
- Lendo os logs, olha que interessante:
  - [main] INFO org.apache.kafka.clients.producer.KafkaProducer - [Producer clientId=producer-1] **Instantiated an idempotent producer.**
- Erro de Metadados: Líder Não Disponível
  - [kafka-producer-network-thread | producer-1] WARN org.apache.kafka.clients.NetworkClient - [Producer clientId=producer-1] Error while fetching metadata with correlation id 1 : {TREINO_ECOMMERCE_NEW_ORDER=LEADER_NOT_AVAILABLE}
  - Descrição: O Kafka Producer tentou obter metadados para o tópico TREINO_ECOMMERCE_NEW_ORDER, mas o líder da partição não está disponível no momento.
  - Contexto: Em Kafka, cada partição de um tópico tem um "líder" responsável por gerenciar as operações. Esse erro indica que o líder da partição não está acessível, o que pode ser devido a uma falha temporária no cluster ou porque o tópico acabou de ser criado e ainda está se estabilizando.
- zookeeper is not a recognized option
  - --zookeeper foi descontinuado: O Kafka não usa mais o Zookeeper diretamente para operações administrativas como alterar o número de partições de um tópico.
  --bootstrap-server localhost:9092: Agora você se conecta diretamente ao broker Kafka usando o endereço do servidor.
- PROP: CLIENT_ID É A INSTÂNCIA DO SERVIÇO
- A chave é usada para distribuir a mensagem entre as partições existentes e consequentemente entre as instâncias de um serviço dentro de um consumer group.

### 2:
O que aprendemos nessa aula:
- Como rodar diversos consumidores no mesmo grupo
- Como paralelizar tarefas
- A importância da chave para hash
- Cuidado com poll longo
