import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Student} from '../models/Student';

@Injectable({
  providedIn: 'root'
})
export class StudentService {
  constructor(private httpClient: HttpClient) { }

  getStudents(): Observable<Array<Student>> {
    return this.httpClient.get<Array<Student>>('/api/students');
  }
}
