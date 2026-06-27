import { Routes } from '@angular/router';
import { Home } from './pages/home/home';
import { SellerDashboard } from './pages/seller-dashboard/seller-dashboard';
import { NotFound } from './pages/not-found/not-found';
import { Login } from './pages/login/login';
import { Register } from './pages/register/register';
import { Profile } from './pages/profile/profile';
import { ProductDetail } from './pages/product-detail/product-detail';
import { guestOnlyGuard, authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
    { path: '', component: Home },
    { path: 'products/:id', component: ProductDetail },
    { path: 'dashboard', component: SellerDashboard, canActivate: [authGuard] },
    { path: 'login', component: Login, canActivate: [guestOnlyGuard] },
    { path: 'register', component: Register, canActivate: [guestOnlyGuard] },
    { path: 'profile', component: Profile, canActivate: [authGuard] },
    { path: 'not-found', component: NotFound },
    { path: '**', redirectTo: 'not-found' }
];
