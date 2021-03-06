import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {AddJwtTokenHttpClient} from '../../../services/add-jwt-token.service';

@Injectable({
  providedIn: 'root'
})
export class ReviewAssignmentChooseService {

  COMMIT_RECORD_DETAIL = environment.SERVER_URL + '/webapi/commits/commitRecords';
  ASSIGNMENT_API = environment.SERVER_URL + '/webapi/assignment/getAssignment';
  GITLAB_URL_API = environment.SERVER_URL + '/webapi/commits/gitLab';
  FEEDBACK_API = environment.SERVER_URL + '/webapi/commits/feedback';
  SCREENSHOT_API = environment.SERVER_URL + '/publicApi/commits/screenshot/getScreenshotURL';
  REVIEW_FEEDBACK_API = environment.SERVER_URL + '/webapi/peerReview/record/detail';
  constructor(private addJwtTokenHttpClient: AddJwtTokenHttpClient) { }

  getCommitDetail(assignmentName: string, username: string): Observable<any> {
    const params = new HttpParams()
      .set('username', username)
      .set('assignmentName', assignmentName);
    return this.addJwtTokenHttpClient.get(this.COMMIT_RECORD_DETAIL, { params });
  }

  getFeedback(assignmentName: string, username: string, commitNumber: string): Observable<any> {
    const params = new HttpParams()
      .set('username', username)
      .set('assignmentName', assignmentName)
      .set('number', commitNumber);
    return this.addJwtTokenHttpClient.get(this.FEEDBACK_API, { params });
  }

  getAssignment(assignmentName: string): Observable<any> {
    const params = new HttpParams()
      .set('assignmentName', assignmentName);
    return this.addJwtTokenHttpClient.get(this.ASSIGNMENT_API, { params });
  }

  getGitAssignmentURL(assignmentName: string, username: string): Observable<any> {
    const params = new HttpParams()
      .set('username', username)
      .set('assignmentName', assignmentName);
    return this.addJwtTokenHttpClient.get(this.GITLAB_URL_API, { params });
  }

  getScreenshotUrls(username: string, assignmentName: string, commitNumber: number): Observable<any> {
    const params = new HttpParams()
      .set('username', username)
      .set('assignmentName', assignmentName)
      .set('commitNumber', commitNumber.toString());
    return this.addJwtTokenHttpClient.get(this.SCREENSHOT_API, { params });
  }

  getReviewFeedback(assignmentName: string, username: string): Observable<any> {
    const params = new HttpParams()
      .set('username', username)
      .set('assignmentName', assignmentName);
    return this.addJwtTokenHttpClient.get(this.REVIEW_FEEDBACK_API, { params });
  }
}
