package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProfilesDumpRunner implements ApplicationRunner {
    @Autowired
    Environment env;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        final Logger log = LoggerFactory.getLogger(this.getClass());
        for (final String defaultProfile : env.getDefaultProfiles()) {
            log.info("default profiles : {}", defaultProfile);
        }
        for (final String activeProfile : env.getActiveProfiles()) {
            log.info("active profiles : {}", activeProfile);
        }
    }
}
