package com.minispring.authservice.config;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executors;
import javax.net.ssl.SSLContext;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class WebConfig {

    @Value("${keycloak.auth-server-url:http://localhost:9000}")
    private String authServerUrl;

    @Value("${keycloak.base-url}")
    private String baseUrl;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.realm}")
    private String realm;

    @Bean
    public RestClient restClient(SslBundles sslBundles) {
        SSLContext sslContext = sslBundles.getBundle("keycloak-mtls").createSslContext();

        HttpClient httpClient = HttpClient.newBuilder()
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .connectTimeout(Duration.ofSeconds(3))
                .version(HttpClient.Version.HTTP_2)
                .sslContext(sslContext)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(authServerUrl)
                .build();
    }

    @Bean
    public Keycloak keycloakClient(SslBundles sslBundles) {
        SSLContext sslContext = sslBundles.getBundle("keycloak-mtls").createSslContext();

        Client jakartaClient = ClientBuilder.newBuilder().sslContext(sslContext).build();

        return KeycloakBuilder.builder()
                .serverUrl(baseUrl)
                .realm(realm)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(clientId)
                .resteasyClient(jakartaClient)
                .build();
    }
}
