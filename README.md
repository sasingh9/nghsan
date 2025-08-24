# Spring Boot MySQL Kafka JSON Demo

This project is a Spring Boot application that listens to a Kafka topic for JSON messages and stores them in a MySQL database.

It uses Spring Boot, Spring Kafka, Spring Data JPA, MySQL, and H2.

## How to Run the Backend

This application uses Spring Profiles to manage database configurations.

### Default Profile (H2 In-Memory)

By default, without specifying a profile, the application will run with an in-memory H2 database. This is useful for quick local testing without needing to set up a separate database.

To run with the default profile:
```bash
mvn spring-boot:run
```

### 'dev' Profile (MySQL)

To run the application with a MySQL database, you need to activate the `dev` profile.

**Prerequisites:**
1.  A running MySQL server on `localhost:3306`.
2.  The application will attempt to connect to a database named `tradedevdb` and create it if it doesn't exist.
3.  The connection uses the username `root` and password `password`. You can change these defaults in `src/main/resources/application-dev.properties`.

To run with the `dev` profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

When you run with the `dev` profile, Hibernate will automatically create or update the necessary tables in your `tradedevdb` database on startup.

## Full-Stack Application (with React UI)

This project is the backend for a full-stack application. To run the entire application with its frontend:

### 1. Run the Backend

Follow the instructions above to start the Spring Boot application, typically using the `dev` profile to connect to a persistent MySQL database. The backend will run on `http://localhost:8080`.

### 2. Run the Frontend

**Important: Frontend Proxy Configuration**

To allow the frontend development server (running on `localhost:3000`) to communicate with the backend (on `localhost:8080`), you must configure a proxy. This bypasses browser CORS (Cross-Origin Resource Sharing) restrictions during development.

If your React application was created with `create-react-app`, add the following line to your frontend's `package.json` file:
```json
  "proxy": "http://localhost:8080"
```

After adding this, **you must restart your React development server**.

With the proxy configured, you can run the frontend:
1.  Make sure you have Node.js and npm installed.
2.  Open a new terminal window.
3.  Navigate to the `frontend` directory.
4.  Install the dependencies: `npm install`
5.  Start the React development server: `npm start`
    This will open the user interface in your web browser, usually at `http://localhost:3000`.

### 3. Using the UI

The React application will load in your browser. Your API calls in the frontend code should be made to relative paths (e.g., `/api/data`), not absolute URLs. The proxy will automatically forward these requests to the backend.
