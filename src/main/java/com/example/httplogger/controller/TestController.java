package com.example.httplogger.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @GetMapping("/log")
    public String generateLog(@RequestParam(defaultValue = "This is a test log message.") String message) {
        logger.info(message);
        return "Log event generated: '" + message + "'";
    }

    @GetMapping("/error")
    public String generateErrorLog() {
        String errorMessage = "This is a test error message.";
        try {
            int result = 10 / 0;
        } catch (Exception e) {
            logger.error(errorMessage, e);
        }
        return "Error log event generated: '" + errorMessage + "'";
    }
}
