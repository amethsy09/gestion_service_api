import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { Role } from '../core/models';

@Component({
  standalone: true,
  imports: [CommonModule, RouterLink, RouterOutlet],
  template: `
    <div class="shell">
      <button *ngIf="menuOpen" class="nav-scrim" aria-label="Fermer le menu" (click)="menuOpen = false"></button>
      <aside class="sidebar" [class.open]="menuOpen" aria-label="Navigation principale">
        <a class="brand" routerLink="/app/accueil" (click)="menuOpen = false">
          <span class="brand-mark small-mark" aria-hidden="true">GS</span>
          <span>Gestion<span class="brand-light">Services</span></span>
        </a>
        <div class="workspace-label">ESPACE DE TRAVAIL</div>
        <nav aria-label="Sections">
          <a *ngFor="let item of visibleLinks" [routerLink]="item.path" routerLinkActive="selected" (click)="menuOpen = false">
            <span class="nav-icon" aria-hidden="true">{{item.icon}}</span><span>{{item.label}}</span>
          </a>
        </nav>
        <div class="side-bottom">
          <div class="user-card">
            <div class="avatar" aria-hidden="true">{{role==='ROLE_ADMIN'?'AD':role==='ROLE_RESPONSIBLE'?'RE':'US'}}</div>
            <div><strong>{{roleLabel}}</strong><small>{{role==='ROLE_ADMIN'?'Administration':'Session active'}}</small></div>
          </div>
          <button class="logout" (click)="auth.logout()"><span aria-hidden="true">↗</span><span>Se déconnecter</span></button>
        </div>
      </aside>

      <main class="main-area">
        <header class="topbar">
          <button class="menu-toggle" aria-label="Ouvrir le menu" [attr.aria-expanded]="menuOpen" (click)="menuOpen = !menuOpen">☰</button>
          <div><span class="crumb">PORTAIL /</span> Gestion des opérations</div>
          <div class="top-user"><span class="status-dot" aria-hidden="true"></span><span>{{roleLabel}}</span></div>
        </header>
        <div class="content"><router-outlet /></div>
      </main>
    </div>
  `
})
export class WorkspaceComponent {
  menuOpen = false;
  readonly links = [
    { label: 'Vue d’ensemble', path: '/app/accueil', icon: '⌂', roles: ['ROLE_USER', 'ROLE_ADMIN', 'ROLE_RESPONSIBLE'] },
    { label: 'Catalogue', path: '/app/services', icon: '▤', roles: ['ROLE_USER', 'ROLE_ADMIN', 'ROLE_RESPONSIBLE'] },
    { label: 'Demandes', path: '/app/demandes', icon: '◷', roles: ['ROLE_USER', 'ROLE_ADMIN'] },
    { label: 'Prestations', path: '/app/prestations', icon: '◈', roles: ['ROLE_ADMIN', 'ROLE_RESPONSIBLE'] },
    { label: 'Tâches', path: '/app/taches', icon: '✓', roles: ['ROLE_ADMIN', 'ROLE_RESPONSIBLE'] },
    { label: 'Ressources', path: '/app/ressources', icon: '♙', roles: ['ROLE_ADMIN', 'ROLE_RESPONSIBLE'] },
    { label: 'Responsables', path: '/app/responsables', icon: '♧', roles: ['ROLE_ADMIN'] },
    { label: 'Spécialités', path: '/app/specialites', icon: '✳', roles: ['ROLE_ADMIN'] },
    { label: 'Paiements', path: '/app/paiements', icon: '＄', roles: ['ROLE_USER'] },
    { label: 'Sécurité', path: '/app/mot-de-passe', icon: '⌑', roles: ['ROLE_USER', 'ROLE_ADMIN', 'ROLE_RESPONSIBLE'] }
  ];
  constructor(readonly auth: AuthService) {}
  get role(): Role | null { return this.auth.role; }
  get roleLabel(): string { return this.role === 'ROLE_ADMIN' ? 'Administrateur' : this.role === 'ROLE_RESPONSIBLE' ? 'Responsable' : 'Utilisateur'; }
  get visibleLinks() { return this.links.filter(item => this.role && item.roles.includes(this.role)); }
}
