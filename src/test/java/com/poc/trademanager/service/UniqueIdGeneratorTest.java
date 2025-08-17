package com.poc.trademanager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniqueIdGeneratorTest {

    private UniqueIdGenerator uniqueIdGenerator;

    @BeforeEach
    void setUp() {
        uniqueIdGenerator = new UniqueIdGenerator();
        uniqueIdGenerator.init(); // Manually call init for the test
    }

    @Test
    void generateUniqueId_shouldContainServerIdentifier() throws UnknownHostException {
        String uniqueId = uniqueIdGenerator.generateUniqueId();
        assertNotNull(uniqueId);

        String expectedPrefix = InetAddress.getLocalHost().getHostName();
        assertTrue(uniqueId.startsWith(expectedPrefix + "-"));
    }
}
