package com.mishraachandan.booking_system.config;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Simple in-memory, per-key sliding-window rate limiter.
 *
 * <p>This is deliberately lightweight — no external dependency — and is
 * sufficient for protecting authentication endpoints (login, OTP verify) from
 * casual brute-force attacks on a single-instance deployment. For a clustered
 * deployment, swap this out for a distributed limiter (Redis / Bucket4j).
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
        Deque<Instant> deque = hits.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        synchronized (deque) {
            Iterator<Instant> it = deque.iterator();
            while (it.hasNext()) {
                if (it.next().isBefore(cutoff)) {
                    it.remove();
                } else {
                    break;
                }
            }

            if (deque.size() >= maxHits) {
                return false;
            }
            deque.addLast(now);
            return true;
        }
    }
}
