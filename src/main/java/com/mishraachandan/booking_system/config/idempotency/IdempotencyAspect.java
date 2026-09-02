package com.mishraachandan.booking_system.config.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mishraachandan.booking_system.config.AuthenticatedUser;
import com.mishraachandan.booking_system.service.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyAspect {

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(idempotentAnnotation)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotentAnnotation) throws Throwable {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attrs.getRequest();
        String headerName = idempotentAnnotation.headerName();
        String idempotencyKey = request.getHeader(headerName);

        // If client did not provide an Idempotency-Key header, proceed normally
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return joinPoint.proceed();
        }

        idempotencyKey = idempotencyKey.trim();
        Long userId = extractCurrentUserId();
        String path = request.getRequestURI();
        long expireMinutes = idempotentAnnotation.expireAfterMinutes();

        // Check if already processed or in-flight
        Optional<IdempotencyService.CachedResponse> cachedOpt =
                idempotencyService.startOrGet(idempotencyKey, userId, path, expireMinutes);

        if (cachedOpt.isPresent()) {
            IdempotencyService.CachedResponse cached = cachedOpt.get();
            return buildResponseFromCache(joinPoint, cached);
        }

        try {
            Object result = joinPoint.proceed();

            if (result instanceof ResponseEntity<?> responseEntity) {
                int status = responseEntity.getStatusCode().value();
                String bodyJson = objectMapper.writeValueAsString(responseEntity.getBody());
                idempotencyService.markCompleted(idempotencyKey, status, bodyJson, expireMinutes);
            } else if (result != null) {
                String bodyJson = objectMapper.writeValueAsString(result);
                idempotencyService.markCompleted(idempotencyKey, 200, bodyJson, expireMinutes);
            }

            return result;
        } catch (Throwable t) {
            idempotencyService.markFailed(idempotencyKey);
            throw t;
        }
    }

    private Object buildResponseFromCache(ProceedingJoinPoint joinPoint, IdempotencyService.CachedResponse cached) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Type returnType = method.getGenericReturnType();

        try {
            if (returnType instanceof ParameterizedType paramType && paramType.getRawType().equals(ResponseEntity.class)) {
                Type bodyType = paramType.getActualTypeArguments()[0];
                Object bodyObj = objectMapper.readValue(cached.getResponseBody(), objectMapper.constructType(bodyType));
                return ResponseEntity.status(cached.getStatusCode()).body(bodyObj);
            } else {
                return objectMapper.readValue(cached.getResponseBody(), method.getReturnType());
            }
        } catch (Exception e) {
            log.error("Failed to deserialize cached idempotency response: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to restore idempotent response", e);
        }
    }

    private Long extractCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
                return user.getUserId();
            }
        } catch (Exception ignored) {}
        return null;
    }
}
