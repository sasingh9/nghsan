# Spring Boot Oracle Kafka JSON Demo

This project is a Spring Boot application that listens to a Kafka topic for JSON messages and stores them in an Oracle 19c (or later) database.

It uses Spring Boot, Spring Kafka, Spring JDBC (`JdbcTemplate`), and the Oracle JDBC driver.

## How to Configure

1.  Open the `src/main/resources/application.properties` file.
2.  Update the Oracle database connection details:
    ```properties
    spring.datasource.url=jdbc:oracle:thin:@//your-db-host:1521/your-service-name
    spring.datasource.username=your_username
    spring.datasource.password=your_password
    ```
3.  Update the Kafka consumer configuration:
    ```properties
    spring.kafka.consumer.bootstrap-servers=your-kafka-broker:9092
    spring.kafka.consumer.group-id=your-consumer-group
    app.kafka.topic.json-input=your-kafka-topic
    ```

## How to Run

1.  Make sure you have Java 11 (or later) and Maven installed.
2.  Ensure you have access to a running Oracle database and a Kafka broker.
3.  Open a terminal or command prompt in the root directory of the project.
4.  Run the application using the Spring Boot Maven plugin:

    ```bash
    mvn spring-boot:run
    ```

## What it Does

When you run the application, it will:
1.  Connect to the Oracle database.
2.  On first startup, it will drop and recreate the `json_docs` table.
3.  Connect to the configured Kafka broker.
4.  Listen for messages on the specified Kafka topic (`app.kafka.topic.json-input`).
5.  When a message is received, it will save the message content as a new row in the `json_docs` table in the database.

The application will continue running to listen for new messages. You can stop it with `Ctrl+C`.
