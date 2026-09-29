import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { Role } from './models';
export const authGuard: CanActivateFn = route => { const auth = inject(AuthService); const router = inject(Router); if (!auth.token) return router.createUrlTree(['/connexion']); const roles = route.data['roles'] as Role[] | undefined; return !roles || auth.hasRole(roles) ? true : router.createUrlTree(['/app/accueil']); };
