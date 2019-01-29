package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpPortInitializedListener {

    private int httpPort;

    public HttpPortInitializedListener() {
    }

    public void setHttpPort(final int v) {
        final Logger log = LoggerFactory.getLogger(this.getClass());
        this.httpPort = v;
        log.info("http-port = " + this.httpPort);
        log.info(this.getClass().getClassLoader().toString());
    }

    public int getHttpPort() {
        final Logger log = LoggerFactory.getLogger(this.getClass());
        log.info("http-port = " + this.httpPort);
        log.info(this.getClass().getClassLoader().toString());
        return httpPort;
    }
}
