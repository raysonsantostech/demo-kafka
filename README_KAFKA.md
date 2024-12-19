# Execução do Zookeeper
> bin/zookeeper-server-start.sh config/zookeeper.properties 

# Execução Kafka Server
> bin/kafka-server-start.sh config/server.properties

# Criação de Tópico
> bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic hello-topic 

# Listar Tópicos
> bin/kafka-topics.sh --list --bootstrap-server localhost:9092

# Producer CLI
> bin/kafka-console-producer.sh --broker-list localhost:9092 --topic hello-topic 

# Consumer CLI

- **A partir deste momento (latest)**
> bin/kafka-console-consumer.sh --broker-list --bootstrap-server localhost:9092 --topic hello-topic

- **Començando do Começo (earliest)**
> bin/kafka-console-consumer.sh --broker-list --bootstrap-server localhost:9092 --topic hello-topic --from-beginning

#

#

#

#
