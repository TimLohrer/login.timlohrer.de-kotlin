FROM adoptopenjdk:17-jre-hotspot

WORKDIR /app
COPY build/libs/timlohrer.de.login-0.0.1.jar /app/

CMD ["java", "-jar", "timlohrer.de.login-0.0.1.jar"]