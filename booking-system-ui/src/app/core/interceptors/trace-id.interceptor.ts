import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Injects a unique X-Trace-Id header on every outgoing request for server-side MDC logging.
 * NOTE: X-User-Id is no longer injected here — userId is now carried securely in the JWT.
 */
export const traceIdInterceptor: HttpInterceptorFn = (req, next) => {
  const traceId = crypto.randomUUID().replace(/-/g, '').substring(0, 16);

  const cloned = req.clone({
    headers: req.headers.set('X-Trace-Id', traceId),
  });

  return next(cloned);
};
