package com.mishraachandan.booking_system.config.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller endpoint as idempotent.
 * Requires the client to supply an `Idempotency-Key` header (e.g. a UUID).
 * Duplicate requests with the same key will return the exact cached response
 * without executing the underlying business logic multiple times.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * HTTP Header containing the unique idempotency key.
     */
    String headerName() default "Idempotency-Key";

    /**
     * Time-to-live for the idempotency record in minutes (default 24 hours).
     */
    long expireAfterMinutes() default 1440;
}
