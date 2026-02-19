FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/ledger-service-0.1.0.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
