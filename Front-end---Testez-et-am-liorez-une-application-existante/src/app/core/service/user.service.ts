import { Injectable } from '@angular/core';
import { Register } from '../models/Register';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {Login} from '../models/Login';
import {LoginResponse} from '../models/LoginResponse';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  constructor(private httpClient: HttpClient) { }

  register(user: Register): Observable<Object> {
    return this.httpClient.post('/api/register', user);
  }

  login(user: Login): Observable<LoginResponse> {
    return this.httpClient.post<LoginResponse>('/api/login', user);
  }
}
