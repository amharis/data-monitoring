#FROM maven:3.6.3-jdk-11-openj9
FROM openjdk:11

WORKDIR /consumer-service
COPY target/java-consumer-1.0-SNAPSHOT-jar-with-dependencies.jar .
ENV LOCAL_INTERFACE=$LOCAL_INTERFACE
ENTRYPOINT ["java", "-jar", "java-consumer-1.0-SNAPSHOT-jar-with-dependencies.jar"]