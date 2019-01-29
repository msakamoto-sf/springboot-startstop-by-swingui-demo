package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

/**
 * @see https://docs.spring.io/spring-boot/docs/2.1.2.RELEASE/reference/htmlsingle/#howto-discover-the-http-port-at-runtime
 * @see https://stackoverflow.com/questions/30312058/spring-boot-how-to-get-the-running-port
 */
@Service
public class HttpPortService implements ApplicationListener<ServletWebServerInitializedEvent> {

    private int httpPort;

    @Override
    public void onApplicationEvent(ServletWebServerInitializedEvent event) {
        this.httpPort = event.getApplicationContext().getWebServer().getPort();
        final Logger log = LoggerFactory.getLogger(this.getClass());
        log.info("application-event : running http port = {}", this.httpPort);
        StaticGlobalRefs.getHttpPortInitializedListener().setHttpPort(httpPort);
    }

    public int getRunningHttpPort() {
        return httpPort;
    }
}
