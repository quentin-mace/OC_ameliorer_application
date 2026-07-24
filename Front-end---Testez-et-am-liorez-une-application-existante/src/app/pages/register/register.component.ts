import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MaterialModule } from '../../shared/material.module';
import { UserService } from '../../core/service/user.service';
import { Register } from '../../core/models/Register';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {Router} from '@angular/router';
import {HttpErrorResponse} from '@angular/common/http';
import {FlashMessageService} from '../../core/service/flash-message.service';

@Component({
  selector: 'app-register',
  imports: [CommonModule, MaterialModule],
  templateUrl: './register.component.html',
  standalone: true,
  styleUrl: './register.component.css'
})
export class RegisterComponent implements OnInit {
  private userService = inject(UserService);
  private formBuilder = inject(FormBuilder);
  private destroyRef = inject(DestroyRef);
  private router = inject(Router);
  private flashMessageService = inject(FlashMessageService);
  registerForm: FormGroup = new FormGroup({});
  submitted: boolean = false;
  errorMessage: string | null = null;

  ngOnInit() {
    this.registerForm = this.formBuilder.group(
      {
        firstName: ['', Validators.required],
        lastName: ['', Validators.required],
        login: ['', Validators.required],
        password: ['', Validators.required]
      },
    );
  }

  get form() {
    return this.registerForm.controls;
  }

  onSubmit(): void {
    this.submitted = true;
    if (this.registerForm.invalid) {
      return;
    }
    this.errorMessage = null;

    const registerUser: Register = {
      firstName: this.registerForm.get('firstName')?.value,
      lastName: this.registerForm.get('lastName')?.value,
      login: this.registerForm.get('login')?.value,
      password: this.registerForm.get('password')?.value
    };
    this.userService.register(registerUser)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.flashMessageService.set('Inscription réussie, vous pouvez vous connecter.')
          this.router.navigate(['/login']);
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage = error.status === 400
            ? "Cet identifiant est déja utilisé"
            : "Une erreur est survenue"
        }
      })
  }

  onReset(): void {
    this.submitted = false;
    this.registerForm.reset();
  }
}
