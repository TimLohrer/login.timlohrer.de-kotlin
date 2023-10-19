FROM openjdk:17-jdk-alpine

WORKDIR /app
COPY build/libs/timlohrer.de.login-0.0.1.jar /app/

CMD ["java", "-jar", "timlohrer.de.login-0.0.1.jar"]