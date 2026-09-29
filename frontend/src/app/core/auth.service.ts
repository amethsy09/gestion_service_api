import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthResponse, Role } from './models';
import { environment } from '../environment';
@Injectable({ providedIn: 'root' }) export class AuthService {
  private readonly key = 'gestion.auth';
  private current = signal<AuthResponse | null>(this.read());
  readonly session = this.current.asReadonly();
  constructor(private http: HttpClient, private router: Router) {}
  private read(): AuthResponse | null { try { const v = sessionStorage.getItem(this.key); return v ? JSON.parse(v) as AuthResponse : null; } catch { return null; } }
  get token(): string | null { const a = this.current(); if (!a) return null; try { const part = a.token.split('.')[1]; const payload = JSON.parse(atob(part.replace(/-/g,'+').replace(/_/g,'/'))) as { exp?: number }; if (!payload.exp || payload.exp * 1000 <= Date.now()) { this.logout(false); return null; } } catch { this.logout(false); return null; } return a.token; }
  get role(): Role | null { return this.current()?.role ?? null; }
  hasRole(roles: Role[]): boolean { return !!this.role && roles.includes(this.role); }
  login(data: { telephone: string; password: string }): Observable<AuthResponse> { return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, data).pipe(tap(a => this.store(a))); }
  register(data: { fullName: string; telephone: string; email: string; password: string }): Observable<AuthResponse> { return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/register`, data).pipe(tap(a => this.store(a))); }
  private store(a: AuthResponse): void { sessionStorage.setItem(this.key, JSON.stringify(a)); this.current.set(a); }
  logout(navigate = true): void { sessionStorage.removeItem(this.key); this.current.set(null); if (navigate) void this.router.navigate(['/connexion']); }
}
