import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token;
  const isAuthRequest = /\/auth\/(login|register|first-change-password)$/.test(req.url);
  const protectedApiRequest = req.url.includes('/api/v1/') && !isAuthRequest;
  const outgoing = token && protectedApiRequest ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
  return next(outgoing).pipe(catchError(error => {
    if (error.status === 401 && protectedApiRequest) auth.logout();
    return throwError(() => error);
  }));
};
