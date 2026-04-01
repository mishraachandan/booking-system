import { HttpInterceptorFn } from '@angular/common/http';

export const traceIdInterceptor: HttpInterceptorFn = (req, next) => {
  const traceId = crypto.randomUUID().replace(/-/g, '').substring(0, 16);
  const userId = localStorage.getItem('userId');

  let headers = req.headers.set('X-Trace-Id', traceId);
  if (userId) {
    headers = headers.set('X-User-Id', userId);
  }

  const cloned = req.clone({ headers });
  return next(cloned);
};
