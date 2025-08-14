# Spring Boot Oracle JSON Demo

This project is a simple Spring Boot command-line application that demonstrates how to store and retrieve a JSON document in an Oracle 19c (or later) database.

It uses Spring Boot with Spring JDBC (`JdbcTemplate`) and the Oracle JDBC driver.

## How to Configure

1.  Open the `src/main/resources/application.properties` file.
2.  Update the following properties with your Oracle database connection details:
    ```properties
    spring.datasource.url=jdbc:oracle:thin:@//your-db-host:1521/your-service-name
    spring.datasource.username=your_username
    spring.datasource.password=your_password
    ```

## How to Run

This application is a command-line application, not a web app. It will perform its database operations and then exit.

1.  Make sure you have Java 11 (or later) and Maven installed.
2.  Open a terminal or command prompt in the root directory of the project.
3.  Run the application using the Spring Boot Maven plugin:

    ```bash
    mvn spring-boot:run
    ```

## What it Does

When you run the application, it will:
1.  Connect to the Oracle database using the credentials in `application.properties`.
2.  Drop the `json_docs` table if it already exists.
3.  Create a new `json_docs` table with a `BLOB` column to store JSON.
4.  Insert a sample JSON document: `{"name":"John Doe","email":"john.doe@example.com","age":30}`.
5.  Retrieve the document from the database.
6.  Print the retrieved JSON to the console.

You will see log output in your console detailing these steps.
