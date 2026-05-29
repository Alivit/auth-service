package com.minispring.authservice;

import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class BaseIntegrationTest {

    @MockitoBean
    protected JwtDecoder jwtDecoder;

    protected static final Network network = Network.newNetwork();

    protected static final PostgreSQLContainer keycloakDb = new PostgreSQLContainer(
                    DockerImageName.parse("postgres:18-alpine"))
            .withNetwork(network)
            .withNetworkAliases("keycloak_postgres")
            .withDatabaseName("kc_test_db")
            .withUsername("fake_db_user")
            .withPassword("fake_db_pass");

    protected static final GenericContainer<?> keycloak = new GenericContainer<>(
                    DockerImageName.parse("quay.io/keycloak/keycloak:26.6"))
            .withNetwork(network)
            .withCommand("start-dev", "--import-realm")
            .withEnv("KC_DB", "postgres")
            .withEnv("KC_DB_URL", "jdbc:postgresql://keycloak_postgres:5432/kc_test_db")
            .withEnv("KC_DB_USERNAME", "fake_db_user")
            .withEnv("KC_DB_PASSWORD", "fake_db_pass")
            .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
            .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
            .withEnv("APP_CLIENT_ID", "test-auth-client")
            .withEnv("APP_SECURITY_TOKEN_LIFESPAN", "300")
            .withEnv("APP_ADMIN_USERNAME", "testadmin")
            .withEnv("APP_ADMIN_EMAIL", "test@test.com")
            .withEnv("APP_ADMIN_PASSWORD", "test")
            .withEnv("KC_HTTPS_CERTIFICATE_FILE", "/opt/keycloak/conf/keycloak.crt")
            .withEnv("KC_HTTPS_CERTIFICATE_KEY_FILE", "/opt/keycloak/conf/keycloak.key")
            .withEnv("KC_TRUSTSTORE_PATHS", "/opt/keycloak/conf/root-ca.crt")
            .withEnv("KC_HTTPS_CLIENT_AUTH", "request")
            .withExposedPorts(8443)
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("certs/keycloak.crt"), "/opt/keycloak/conf/keycloak.crt")
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("certs/keycloak.key"), "/opt/keycloak/conf/keycloak.key")
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("certs/root-ca.crt"), "/opt/keycloak/conf/root-ca.crt")
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("keycloak/test-realm-export.json"),
                    "/opt/keycloak/data/import/dev-realm.json")
            .dependsOn(keycloakDb)
            .waitingFor(Wait.forLogMessage(".*Keycloak.*started.*|.*Listening on:.*", 1)
                    .withStartupTimeout(Duration.ofMinutes(5)));

    static {
        keycloakDb.start();
        keycloak.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String keycloakTestUrl = "https://" + keycloak.getHost() + ":" + keycloak.getMappedPort(8443);

        registry.add("keycloak.base-url", () -> keycloakTestUrl);
        registry.add("keycloak.auth-server-url", () -> keycloakTestUrl + "/realms/dev/protocol/openid-connect");
        registry.add("keycloak.client-id", () -> "test-auth-client");

        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> keycloakTestUrl + "/realms/dev");
        registry.add(
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> keycloakTestUrl + "/realms/dev/protocol/openid-connect/certs");
        registry.add("spring.security.oauth2.resourceserver.jwt.audience", () -> "test-auth-client");

        registry.add(
                "spring.ssl.bundle.pem.keycloak-mtls.keystore.certificate", () -> "classpath:certs/auth-client.crt");
        registry.add(
                "spring.ssl.bundle.pem.keycloak-mtls.keystore.private-key", () -> "classpath:certs/auth-client.key");
        registry.add("spring.ssl.bundle.pem.keycloak-mtls.truststore.certificate", () -> "classpath:certs/root-ca.crt");
    }

    @AfterEach
    void resetMocks() {
        Mockito.reset(jwtDecoder);
    }
}
