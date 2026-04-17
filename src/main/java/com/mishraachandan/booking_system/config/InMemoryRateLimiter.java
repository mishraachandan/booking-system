package com.mishraachandan.booking_system.config;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory, per-key sliding-window rate limiter.
 *
 * <p>This is deliberately lightweight — no external dependency — and is
 * sufficient for protecting authentication endpoints (login, OTP verify) from
 * casual brute-force attacks on a single-instance deployment. For a clustered
 * deployment, swap this out for a distributed limiter (Redis / Bucket4j).
 *
 * <p><strong>Memory safety.</strong> Since rate-limit keys can include
 * attacker-controlled data (e.g. email addresses from the request body), the
 * backing map must evict stale entries — otherwise an attacker can inflate the
 * map by sending requests with millions of unique identifiers (a DoS vector).
 * {@link #tryAcquire(String, int, Duration)} uses
 * {@link ConcurrentHashMap#compute(Object, java.util.function.BiFunction)} to
 * atomically prune expired timestamps and drop the map entry when its deque is
 * empty, which prevents that growth.
 */
@Component
public class InMemoryRateLimiter {

    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();

    /**
     * Record a request for {@code key} and decide whether it should be
     * allowed under the given policy.
     *
     * @param key      identifier for the bucket (e.g. {@code "login:user@x.com"})
     * @param maxHits  maximum number of hits allowed inside the window
     * @param window   size of the sliding window
     * @return {@code true} if the request is allowed, {@code false} if the
     *         caller has exceeded the limit.
     */
    public boolean tryAcquire(String key, int maxHits, Duration window) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(window);
        boolean[] allowed = new boolean[]{false};

        hits.compute(key, (k, existing) -> {
            Deque<Instant> deque = existing == null ? new ArrayDeque<>() : existing;

            Iterator<Instant> it = deque.iterator();
            while (it.hasNext()) {
                if (it.next().isBefore(cutoff)) {
                    it.remove();
                } else {
                    break;
                }
            }

            if (deque.size() >= maxHits) {
                allowed[0] = false;
            } else {
                deque.addLast(now);
                allowed[0] = true;
            }

            // Drop the map entry entirely if nothing is tracked any more —
            // keeps the map bounded even under adversarial key churn.
            return deque.isEmpty() ? null : deque;
        });

        return allowed[0];
    }

    /** Exposed for tests / diagnostics. */
    int trackedKeyCount() {
        return hits.size();
    }
}
