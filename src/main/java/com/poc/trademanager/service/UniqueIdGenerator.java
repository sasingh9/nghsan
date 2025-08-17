package com.poc.trademanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

@Service
public class UniqueIdGenerator {

    private static final Logger log = LoggerFactory.getLogger(UniqueIdGenerator.class);

    private String serverIdentifier;

    @PostConstruct
    public void init() {
        try {
            serverIdentifier = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.warn("Could not determine hostname for unique ID generation. Falling back to a random identifier.", e);
            serverIdentifier = "unknown-" + UUID.randomUUID().toString().substring(0, 8);
        }
        log.info("Unique ID generator initialized with server identifier: {}", serverIdentifier);
    }

    public String generateUniqueId() {
        return serverIdentifier + "-" + UUID.randomUUID().toString();
    }
}
