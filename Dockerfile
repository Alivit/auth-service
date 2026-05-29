FROM gradle:jdk25 AS build
WORKDIR /app

ENV GRADLE_OPTS="-Xmx1536m -XX:MaxMetaspaceSize=384m -Dorg.gradle.jvmargs=-Xmx1536m"

COPY build.gradle settings.gradle ./
RUN --mount=type=cache,target=/home/gradle/.gradle/caches,sharing=locked \
    gradle dependencies --no-daemon --max-workers=1

COPY src ./src
RUN --mount=type=cache,target=/home/gradle/.gradle/caches,sharing=locked \
    gradle bootJar --no-daemon -x test --max-workers=1

RUN mv build/libs/$(ls build/libs/ | grep -v plain) /app/app.jar

FROM eclipse-temurin:25-jdk AS optimizer
WORKDIR /app
COPY --from=build /app/app.jar app.jar
RUN jar -xf app.jar && rm app.jar

RUN java -XX:DumpLoadedClassList=classes.list -jar app.jar || true
RUN java -XX:SharedClassListFile=classes.list -XX:SharedArchiveFile=app.jsa -Xshare:dump -jar app.jar || true

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring --no-create-home spring
USER spring:spring

COPY --from=build /app/app.jar ./app.jar

EXPOSE 8082
EXPOSE 9090

ENTRYPOINT [ \
    "java", \
    "-XX:+UseG1GC", \
    "-jar", "app.jar" \
]