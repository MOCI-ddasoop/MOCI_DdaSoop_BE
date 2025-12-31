package com.back;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "TOSS_SECRET_KEY=test-secret-key",
    "TOSS_CLIENT_KEY=test-client-key"
})
class ApplicationTests {

    @Test
    void contextLoads() {
    }

}
