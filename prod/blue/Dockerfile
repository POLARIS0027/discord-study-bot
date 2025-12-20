FROM eclipse-temurin:17-jre-jammy

RUN echo "Building for: $(uname -m)"

WORKDIR /app

COPY build/libs/*.jar app.jar

RUN mkdir -p /app/logs

RUN apt-get update && apt-get install -y procps && rm -rf /var/lib/apt/lists/*

ENV JAVA_OPTS="-Xms512m -Xmx1536m"

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -Dspring.profiles.active=prod -jar /app/app.jar"]

