# Spring Boot MySQL Kafka JSON Demo

This project is a Spring Boot application that listens to a Kafka topic for JSON messages and stores them in a MySQL database.

It uses Spring Boot, Spring Kafka, Spring Data JPA, and the MySQL JDBC driver.

## How to Configure

1.  Open the `src/main/resources/application.properties` file.
2.  Update the MySQL database connection details:
    ```properties
    spring.datasource.url=jdbc:mysql://localhost:3306/tradedevdb?createDatabaseIfNotExist=true
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
2.  Ensure you have access to a running MySQL database and a Kafka broker.
3.  Open a terminal or command prompt in the root directory of the project.
4.  Run the application using the Spring Boot Maven plugin:

    ```bash
    mvn spring-boot:run
    ```

## What it Does

When you run the application, it will:
1.  Connect to the MySQL database.
2.  On first startup, it will create the necessary tables.
3.  Connect to the configured Kafka broker and listen for messages to store in the database.
4.  Start a web server on port 8080.
5.  Expose a REST API endpoint at `GET /api/data` for retrieving stored JSON data.

The application will continue running to listen for new messages and serve API requests. You can stop it with `Ctrl+C`.

## Full-Stack Application (with React UI)

This project now includes a React frontend for viewing the data. To run the full application:

### 1. Run the Backend

Follow the instructions in the "How to Run" section above to start the Spring Boot application. The backend will run on `http://localhost:8080`.

### 2. Run the Frontend

1.  Make sure you have Node.js and npm installed.
2.  Open a new terminal window.
3.  Navigate to the `frontend` directory:
    ```bash
    cd frontend
    ```
4.  Install the dependencies:
    ```bash
    npm install
    ```
5.  Start the React development server:
    ```bash
    npm start
    ```
    This will open the user interface in your web browser, usually at `http://localhost:3000`.

### 3. Using the UI

The React application will load in your browser. You can use the date and time pickers to select a date range and click "Fetch Data" to see the JSON records stored in the database within that timeframe.
