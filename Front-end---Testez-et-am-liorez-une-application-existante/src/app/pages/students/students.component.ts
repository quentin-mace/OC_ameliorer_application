import {Component, inject, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MaterialModule} from '../../shared/material.module';
import {Student} from '../../core/models/Student';
import {StudentService} from '../../core/service/student.service';
import {Router} from '@angular/router';

@Component({
  selector: 'app-students',
  imports: [CommonModule, MaterialModule],
  templateUrl: './students.component.html',
  styleUrl: './students.component.css'
})
export class StudentsComponent implements OnInit {
  private studentService: StudentService = inject(StudentService);
  private router = inject(Router);
  errorMessage: string | null = null;
  students: Student[] = [];

  ngOnInit() {
    this.errorMessage = null;
    this.studentService.getStudents().subscribe({
      next : students => this.students = students,
      error: (error) => {
        if (error.status === 401) {
          this.router.navigate(['/login']);
        } else {
          this.errorMessage = "Une erreur est survenue"
        }
      }
    });
  }
}
