package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    @Autowired
    HttpPortService httpPortService;

    @GetMapping(path = "/")
    public String index(final Model model) {
        final Logger log = LoggerFactory.getLogger(this.getClass());
        model.addAttribute("name", "springboot");
        log.info("hello!!");
        log.info("running http port = {}", httpPortService.getRunningHttpPort());
        return "index";
    }
}
