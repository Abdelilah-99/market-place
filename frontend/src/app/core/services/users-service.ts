import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, from, switchMap, throwError } from 'rxjs';

export interface LoginPayload {
  identification: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  role: string;
  message: string;
}

export interface avatarUpdate {
  oldAvatar: string;
  newAvatar: string;
}

export interface RegisterPayload {
  email: string;
  name: string;
  password: string;
  role: string;
  avatarUrl: string;
}

export interface Me {
  id: string;
  username: string;
  email: string;
  role: string;
  avatarUrl: string | null;
}

export interface UpdateProfile {
  name: string;
  email: string;
  uuid: string | null;
}

export interface ApiMessageResponse {
  msg: string;
}

export interface PageResponse<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
  totalPages: number;
  hasNext: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class UsersService {
  private readonly loginPath = '/api/users/login';
  private readonly registerPath = '/api/users/register';
  private readonly mediaPath = '/api/media/users';
  private readonly deletePath = '/api/users/me'

  constructor(private http: HttpClient) { }

  loginUser(userData: LoginPayload): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(this.loginPath, userData);
  }

  meUser(): Observable<Me> {
    return this.http.get<Me>(`/api/users/me`);
  }

  registerUser(userData: RegisterPayload): Observable<ApiMessageResponse> {
    // const formData = new FormData();
    // const infoBlob = new Blob([JSON.stringify(userData)], {
    //   type: 'application/json'
    // });

    // formData.append('info', infoBlob);
    console.log(userData.avatarUrl);
    

    return this.http.post<ApiMessageResponse>(this.registerPath, userData);
  }

  getAvatar(avatar: string): Observable<Blob> {
    return this.http.get(`${this.mediaPath}/${avatar}`, {
      responseType: 'blob',
    });
  }

  registerUserWithAvatar(avatar?: File | null): Observable<ApiMessageResponse> {
    const formData = new FormData();
    // const infoBlob = new Blob([JSON.stringify(userData)], {
    //   type: 'application/json'
    // });

    // formData.append('info', infoBlob);
    if (avatar) {
      formData.append('avatar', avatar, avatar.name);
    }

    return this.http.post<ApiMessageResponse>(this.mediaPath, formData);
  }

  updateUser(userData: UpdateProfile): Observable<Me> {
    return this.http.put<Me>(`/api/users/me`, userData);
  }

  uploadAvatar(avatar: File): Observable<string> {
    if (!avatar) {
      return throwError(() => new Error('No avatar file provided'));
    }

    return from(avatar.arrayBuffer()).pipe(
      switchMap((bytes: ArrayBuffer) => {
        const headers = new HttpHeaders({
          'Content-Type': avatar.type || 'application/octet-stream',
        });

        return this.http.post(`${this.mediaPath}/`, bytes, {
          headers,
          responseType: 'text' as const,
        });
      })
    );
  }

  logeUser(userData: LoginPayload): Observable<LoginResponse> {
    return this.loginUser(userData);
  }

  deleteUser() {
    return this.http.delete<ApiMessageResponse>(`${this.deletePath}`);
  }

  getAdminUsers(page = 0, size = 12): Observable<PageResponse<Me>> {
    return this.http.get<PageResponse<Me>>('/api/admin/users', {
      params: { page, size },
    });
  }

  searchUsers(query: string, page = 0, size = 8): Observable<PageResponse<Me>> {
    return this.http.get<PageResponse<Me>>('/api/users/search', {
      params: { q: query, page, size },
    });
  }

  updateAdminUserRole(id: string, role: string): Observable<Me> {
    return this.http.patch<Me>(`/api/admin/users/${id}/role`, { role });
  }

  deleteAdminUser(id: string): Observable<ApiMessageResponse> {
    return this.http.delete<ApiMessageResponse>(`/api/admin/users/${id}`);
  }
}
