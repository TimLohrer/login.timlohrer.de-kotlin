FROM openjdk:17-jdk-alpine

WORKDIR /app
COPY build/libs/timlohrer.de.login-all.jar /app/

EXPOSE 8080

CMD ["java", "-jar", "timlohrer.de.login-all.jar"]