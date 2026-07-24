import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { AuthService } from '../service/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.url.includes('/api/login') || req.url.includes('/api/register')) {
    return next(req);
  }

  const authService = inject(AuthService);
  const authReq = req.clone({
    setHeaders: { Authorization: 'Bearer ' + authService.getToken() }
  });

  return next(authReq);
};
