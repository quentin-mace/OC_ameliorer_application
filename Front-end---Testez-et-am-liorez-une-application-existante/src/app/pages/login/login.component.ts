import {Component, DestroyRef, inject, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MaterialModule} from '../../shared/material.module';
import {UserService} from '../../core/service/user.service';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {Register} from '../../core/models/Register';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Login} from '../../core/models/Login';
import {AuthService} from '../../core/service/auth.service';
import {HttpErrorResponse} from '@angular/common/http';

@Component({
  selector: 'app-login',
  imports: [CommonModule, MaterialModule],
  templateUrl: './login.component.html',
  standalone: true,
  styleUrl: './login.component.css'
})
export class LoginComponent implements OnInit {
  private userService = inject(UserService);
  private authService = inject(AuthService);
  private formBuilder = inject(FormBuilder);
  private destroyRef = inject(DestroyRef);
  loginForm: FormGroup = new FormGroup({});
  submitted: boolean = false;
  errorMessage: string | null = null;

  ngOnInit() {
    this.loginForm = this.formBuilder.group(
      {
        login: ['', Validators.required],
        password: ['', Validators.required]
      },
    );
  }

  get form() {
    return this.loginForm.controls;
  }

  onSubmit(): void {
    this.submitted = true;
    if (this.loginForm.invalid) {
      return;
    }
    this.errorMessage = null;

    const loginUser: Login = {
      login: this.loginForm.get('login')?.value,
      password: this.loginForm.get('password')?.value
    };
    this.userService.login(loginUser)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
      next: (response) => {
        this.authService.setToken(response.token);
        alert('SUCCESS!! :-)');
        // todo : redirect vers la liste du crud
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.status === 401
          ? 'Identifiants incorrects'
          : 'Une erreur est survenue'
      }
  });
  }

  onReset(): void {
    this.submitted = false;
    this.loginForm.reset();
  }
}
