package example;

import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PrometheusConfig {

    @Bean
    public ServletRegistrationBean registerPrometheusExporterServlet() {
        DefaultExports.initialize(); // Initialize default JVM metrics
        ServletRegistrationBean registration = new ServletRegistrationBean(new MetricsServlet(), "/prometheus");
        registration.setName("prometheusMetrics");
        return registration;
    }
}
