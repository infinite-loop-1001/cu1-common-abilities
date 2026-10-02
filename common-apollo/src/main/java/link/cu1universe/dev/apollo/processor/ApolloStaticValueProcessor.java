package link.cu1universe.dev.apollo.processor;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.property.PlaceholderHelper;
import link.cu1universe.dev.apollo.annotation.ApolloStaticValue;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.Environment;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Initializes fields annotated with {@link ApolloStaticValue} and refreshes them after Apollo changes.
 */
public class ApolloStaticValueProcessor implements BeanPostProcessor, EnvironmentAware, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApolloStaticValueProcessor.class);
    private static final String APOLLO_BOOTSTRAP_NAMESPACES = "apollo.bootstrap.namespaces";
    private static final String DEFAULT_NAMESPACE = "application";

    private final PlaceholderHelper placeholderHelper = new PlaceholderHelper();
    private final ConversionService conversionService = DefaultConversionService.getSharedInstance();
    private final Map<String, Set<StaticFieldBinding>> bindingsByKey = new ConcurrentHashMap<>();

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        Set<String> namespaces = resolveNamespaces();
        for (String namespace : namespaces) {
            Config config = ConfigService.getConfig(namespace);
            config.addChangeListener(this::onConfigChange);
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        ReflectionUtils.doWithFields(bean.getClass(), this::registerBinding);
        return bean;
    }

    void onConfigChange(ConfigChangeEvent changeEvent) {
        for (String key : changeEvent.changedKeys()) {
            Set<StaticFieldBinding> bindings = bindingsByKey.get(key);
            if (bindings == null) {
                continue;
            }

            ConfigChange change = changeEvent.getChange(key);
            for (StaticFieldBinding binding : bindings) {
                setFieldValue(binding.field(), resolveChangedValue(binding, change));
            }
        }
    }

    private void registerBinding(Field field) {
        ApolloStaticValue annotation = field.getAnnotation(ApolloStaticValue.class);
        if (annotation == null) {
            return;
        }
        validateField(field);

        String placeholder = annotation.value();
        String key = extractSingleKey(field, placeholder);
        StaticFieldBinding binding = new StaticFieldBinding(field, placeholder);
        bindingsByKey.computeIfAbsent(key, ignored -> ConcurrentHashMap.newKeySet()).add(binding);
        setFieldValue(field, environment.resolvePlaceholders(placeholder));
    }

    private Set<String> resolveNamespaces() {
        String configuredNamespaces = environment.getProperty(APOLLO_BOOTSTRAP_NAMESPACES, DEFAULT_NAMESPACE);
        Set<String> namespaces = new LinkedHashSet<>();
        for (String namespace : StringUtils.commaDelimitedListToStringArray(configuredNamespaces)) {
            String trimmedNamespace = namespace.trim();
            if (StringUtils.hasText(trimmedNamespace)) {
                namespaces.add(trimmedNamespace);
            }
        }
        if (namespaces.isEmpty()) {
            namespaces.add(DEFAULT_NAMESPACE);
        }
        return namespaces;
    }

    private void validateField(Field field) {
        if (!Modifier.isStatic(field.getModifiers())) {
            throw new IllegalArgumentException("@ApolloStaticValue can only be used on static fields: "
                    + fieldDescription(field));
        }
        if (Modifier.isFinal(field.getModifiers())) {
            throw new IllegalArgumentException("@ApolloStaticValue cannot be used on final fields: "
                    + fieldDescription(field));
        }
    }

    private String extractSingleKey(Field field, String placeholder) {
        Set<String> keys = placeholderHelper.extractPlaceholderKeys(placeholder);
        if (keys.size() != 1) {
            throw new IllegalArgumentException("@ApolloStaticValue requires exactly one placeholder: "
                    + fieldDescription(field));
        }
        return keys.iterator().next();
    }

    private String resolveChangedValue(StaticFieldBinding binding, ConfigChange change) {
        if (change.getNewValue() != null) {
            return change.getNewValue();
        }
        return environment.resolvePlaceholders(binding.placeholder());
    }

    private void setFieldValue(Field field, String value) {
        try {
            Object convertedValue = convertValue(field, value);
            ReflectionUtils.makeAccessible(field);
            field.set(null, convertedValue);
            LOGGER.info("Apollo static value refreshed: {} = {}", fieldDescription(field), value);
        } catch (Exception exception) {
            LOGGER.error("Unable to refresh Apollo static value for {}. Keeping its current value.",
                    fieldDescription(field), exception);
        }
    }

    private Object convertValue(Field field, String value) {
        if (value == null && field.getType().isPrimitive()) {
            throw new IllegalArgumentException("A primitive field cannot be assigned null");
        }
        if (value == null || field.getType() == String.class) {
            return value;
        }
        if (!conversionService.canConvert(String.class, field.getType())) {
            throw new IllegalArgumentException("Unsupported field type: " + field.getType().getName());
        }
        return conversionService.convert(value, field.getType());
    }

    private String fieldDescription(Field field) {
        return field.getDeclaringClass().getName() + "." + field.getName();
    }

    private static final class StaticFieldBinding {

        private final Field field;
        private final String placeholder;

        private StaticFieldBinding(Field field, String placeholder) {
            this.field = field;
            this.placeholder = placeholder;
        }

        private Field field() {
            return field;
        }

        private String placeholder() {
            return placeholder;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof StaticFieldBinding)) {
                return false;
            }
            StaticFieldBinding that = (StaticFieldBinding) object;
            return field.equals(that.field) && placeholder.equals(that.placeholder);
        }

        @Override
        public int hashCode() {
            return Objects.hash(field, placeholder);
        }
    }
}
