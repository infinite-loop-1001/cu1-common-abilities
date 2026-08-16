package cn.cu1universe.apollo.autoconfigure;

import cn.cu1universe.apollo.processor.ApolloStaticValueProcessor;
import com.ctrip.framework.apollo.spring.annotation.ApolloAnnotationProcessor;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the static-value processor and Apollo's annotation processor when necessary.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ApolloConfigChangeListener.class)
@AutoConfigureAfter(name = "com.ctrip.framework.apollo.spring.boot.ApolloAutoConfiguration")
public class ApolloAutoConfiguration {

    @ConditionalOnBean(ApolloAnnotationProcessor.class)
    @Bean
    public ApolloStaticValueProcessor apolloStaticValueProcessor() {
        return new ApolloStaticValueProcessor();
    }
}
