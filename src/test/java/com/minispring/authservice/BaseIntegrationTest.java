package com.minispring.authservice;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class BaseIntegrationTest {

    protected static final Network network = Network.newNetwork();

    protected static final PostgreSQLContainer keycloakDb =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"))
                    .withNetwork(network)
                    .withNetworkAliases("keycloak_postgres")
                    .withDatabaseName("keycloak_db")
                    .withUsername("keycloak")
                    .withPassword("test");

    protected static final GenericContainer<?> keycloak =
            new GenericContainer<>(DockerImageName.parse("quay.io/keycloak/keycloak:26.6"))
                    .withNetwork(network)
                    .withCommand("start-dev", "--import-realm")
                    .withEnv("KC_DB", "postgres")
                    .withEnv("KC_DB_URL", "jdbc:postgresql://keycloak_postgres:5432/keycloak_db")
                    .withEnv("KC_DB_USER", "keycloak")
                    .withEnv("KC_DB_USERNAME", "keycloak")
                    .withEnv("KC_DB_PASSWORD", "test")
                    .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
                    .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
                    .withEnv("KC_HOSTNAME_STRICT", "false")
                    .withExposedPorts(8080)
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource("keycloak/test-realm-export.json"),
                            "/opt/keycloak/data/import/dev-realm.json"
                    )
                    .dependsOn(keycloakDb)
                    .waitingFor(Wait.forLogMessage(".*Keycloak.*started.*|.*Listening on:.*", 1)
                            .withStartupTimeout(Duration.ofMinutes(5)))
                    .withLogConsumer(outputFrame -> System.out.print("[KEYCLOAK] " + outputFrame.getUtf8String()));

    static {
        keycloakDb.start();
        keycloak.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String keycloakTestUrl = "http://" + keycloak.getHost() + ":" + keycloak.getMappedPort(8080);
        registry.add("keycloak.base-url", () -> keycloakTestUrl);
        registry.add("keycloak.auth-server-url", () -> keycloakTestUrl + "/realms/dev/protocol/openid-connect");
    }
}
