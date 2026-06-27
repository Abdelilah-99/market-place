import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { StateService } from '../services/state-service';

export const adminGuard: CanActivateFn = () => {
  const router = inject(Router);
  const stateService = inject(StateService);
  const user = stateService.currentUserSubject.value;
  const role = user?.role || localStorage.getItem('role') || '';

  if (role === 'ADMIN' || role === 'ROLE_ADMIN') {
    return true;
  }

  return router.createUrlTree(['/']);
};
