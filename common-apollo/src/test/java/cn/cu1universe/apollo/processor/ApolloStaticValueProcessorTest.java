package cn.cu1universe.apollo.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import cn.cu1universe.apollo.annotation.ApolloStaticValue;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class ApolloStaticValueProcessorTest {

    private final ApolloStaticValueProcessor processor = new ApolloStaticValueProcessor();

    @BeforeEach
    void setUp() {
        StaticConfig.timeout = 0;
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "test", Collections.singletonMap("movie.api.timeout", "5000")));
        processor.setEnvironment(environment);
    }

    @Test
    void initializesAndRefreshesStaticField() {
        processor.postProcessBeforeInitialization(new StaticConfig(), "staticConfig");

        assertEquals(5000, StaticConfig.timeout);

        ConfigChange change = new ConfigChange(
                "application",
                "movie.api.timeout",
                "5000",
                "6000",
                PropertyChangeType.MODIFIED);
        processor.onConfigChange(new ConfigChangeEvent(
                "application", Collections.singletonMap("movie.api.timeout", change)));

        assertEquals(6000, StaticConfig.timeout);
    }

    static class StaticConfig {

        @ApolloStaticValue("${movie.api.timeout:1000}")
        static int timeout;
    }
}
