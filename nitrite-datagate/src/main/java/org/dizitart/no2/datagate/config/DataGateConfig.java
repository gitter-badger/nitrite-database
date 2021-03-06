package org.dizitart.no2.datagate.config;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

/**
 * Data Gate server config.
 *
 * @since 1.0
 * @author Anindya Chatterjee
 */
@Slf4j
@Configuration
public class DataGateConfig {

    @Value("${datagate.ssl.key-store}")
    private Resource keyStorePath;

    @Value("${datagate.ssl.key-password}")
    private String keyStorePassword;

    @Value("${datagate.http.port}")
    private int requestHttpPort;

    @Value("${datagate.https.port}")
    private int requestHttpsPort;

    @Bean
    public EmbeddedServletContainerCustomizer servletContainerCustomizer() {
        return new EmbeddedServletContainerCustomizer() {
            @Override
            public void customize(ConfigurableEmbeddedServletContainer container) {
                if (container instanceof JettyEmbeddedServletContainerFactory) {
                    customizeJetty((JettyEmbeddedServletContainerFactory) container);
                }
            }

            private void customizeJetty(JettyEmbeddedServletContainerFactory container) {

                container.addServerCustomizers((JettyServerCustomizer) server -> {

                    // HTTP
                    ServerConnector connector = new ServerConnector(server);
                    connector.setPort(requestHttpPort);

                    // HTTPS
                    if (keyStorePath != null && keyStorePath.exists() && requestHttpsPort != 0) {
                        SslContextFactory sslContextFactory = new SslContextFactory();
                        try {
                            sslContextFactory.setKeyStorePath(keyStorePath.getFile().getPath());
                            sslContextFactory.setKeyStorePassword(keyStorePassword);

                            HttpConfiguration https = new HttpConfiguration();
                            https.addCustomizer(new SecureRequestCustomizer());

                            ServerConnector sslConnector = new ServerConnector(
                                    server,
                                    new SslConnectionFactory(sslContextFactory, "http/1.1"),
                                    new HttpConnectionFactory(https));
                            sslConnector.setPort(requestHttpsPort);

                            server.setConnectors(new Connector[]{connector, sslConnector});
                        } catch (IOException e) {
                            log.error("Error while configuring SSL", e);
                        }
                    } else {
                        server.setConnectors(new Connector[]{connector});
                    }
                });
            }
        };
    }
}
