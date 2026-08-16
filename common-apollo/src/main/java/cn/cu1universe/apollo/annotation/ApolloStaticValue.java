package cn.cu1universe.apollo.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a static field to an Apollo property and refreshes it when the property changes.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApolloStaticValue {

    /**
     * A single Spring-style property placeholder, for example {@code ${movie.api.timeout:5000}}.
     */
    String value();
}
