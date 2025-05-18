FROM maven:3.8.6-openjdk-11 AS build
COPY . /app
WORKDIR /app
RUN mvn clean package -DskipTests

FROM openjdk:11-jre-slim
COPY --from=build /app/target/db-map-service-0.0.1.jar /app.jar
RUN apt-get update && apt-get install -y curl postgresql postgresql-contrib
COPY test.sh /test.sh
RUN chmod +x /test.sh
EXPOSE 8080
USER postgres
ENTRYPOINT ["/test.sh"]