import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { Router } from '@angular/router';
import { FormControl, FormGroup, FormBuilder, Validators } from '@angular/forms';
import { CreateAssignmentService } from './create-assignment.service';
import { ModalDirective } from 'ngx-bootstrap/modal';
import { assignmentTypeEnum } from './assignmentTypeEnum';
import { HttpErrorResponse } from '@angular/common/http';
import * as ClassicEditor from '@ckeditor/ckeditor5-build-classic';
import { SortablejsOptions } from 'ngx-sortablejs';
import { assignmentGradingEnum } from './assignmentGradingEnum.enum';
import { Category, Assessment } from './../review-metrics-management/Category';

@Component({
  selector: 'app-create-assignment',
  templateUrl: './create-assignment.component.html',
  styleUrls: ['./create-assignment.component.scss']
})
export class CreateAssignmentComponent implements OnInit, OnDestroy {
  javaTabStatus: { isOpen: boolean } = { isOpen: false };
  androidTabStatus: { isOpen: boolean } = { isOpen: false };
  webTabStatus: { isOpen: boolean } = { isOpen: false };
  autoAssignmentStatus: { isOpen: boolean } = { isOpen: true };
  peerReviewStatus: { isOpen: boolean } = { isOpen: false };
  disabled: boolean = false;
  isDropup: boolean = true;
  autoClose: boolean = false;
  assignment: FormGroup;
  SERVER_URL: string = environment.SERVER_URL;

  errorResponse: HttpErrorResponse;
  errorTitle: string;

  max: number = 100;
  showWarning: boolean;
  dynamic: number = 60;
  type: string = 'Waiting';
  finalIndex: number;
  showAssessment: boolean = true;

  reviewMetricsNums = [0, 1, 2];
  assessments: Assessment[][] = [[], [], []];
  categories: Category[];
  onSelectedCategory: number[] = [0, 1, 2];
  onSelectedMetrics: number[] = [0, 0, 0];

  javaStatusScore = new Map([["Compile Failure", "0"]]);
  webStatusScore = new Map();

  javaStatus = [
    "Unit Test Failure",
    "Coding Style Failure"
  ];

  webStatus = [
    "HTML Failure",
    "CSS Failure",
    "JavaScript Failure",
    "Unit Test Failure"
  ];

  appStatus = [
    "Coding Style Failure",
    "Unit Test Failure",
    "E2E Test Failure"
  ];

  javaOrder = [];
  webOrder = [];

  normalOptions: SortablejsOptions = {
    group: 'normal-group',
  };

  public Editor = ClassicEditor;
  public editorConfig = {
    placeholder: 'Write the assignment description in here!',
    ckfinder: {
      // Upload the images to the server using the CKFinder QuickUpload command.
      uploadUrl: environment.SERVER_URL + `/webapi/image`
    }
  };

  constructor(private router: Router, private fb: FormBuilder, private createService: CreateAssignmentService) { }
  @ViewChild('myModal', { static: true }) public progressModal: ModalDirective;
  @ViewChild('errorModal', { static: false }) public errorModal: ModalDirective;

  ngOnInit() {
    const now_time = Date.now() - (new Date().getTimezoneOffset() * 60 * 1000);
    this.assignment = this.fb.group({
      name: [undefined, [Validators.required, Validators.pattern('^[a-zA-Z0-9-_]{3,10}')]],
      releaseTime: [new Date(now_time).toISOString().slice(0, 17) + '00', Validators.required],
      deadline: [new Date(now_time).toISOString().slice(0, 17) + '00', Validators.required],
      reviewReleaseTime: [new Date(now_time).toISOString().slice(0, 17) + '00', Validators.required],
      reviewDeadline: [new Date(now_time).toISOString().slice(0, 17) + '00', Validators.required],
      commitRecordCount: [0, [Validators.pattern('^[0-9]{1,3}'), Validators.required]],
      description: [undefined, Validators.required],
      type: [undefined, Validators.required],
      file: [undefined, Validators.required],
      method: [assignmentGradingEnum['Auto']],
      assOrder: [undefined],
      rememberMe: [true]
    });
    this.onChanges();
    this.createService.getAllCategory().subscribe(
      response => {
        this.categories = response['allCategory'];
        this.getAllMetrics();
      }
    );
  }

  setCategory(selectedReviews: number, selectedCategory: number): void {
    this.onSelectedCategory[selectedReviews] = selectedCategory;
  }
  setMetrics(selectedReviews: number, selectedMetrics: number): void {
    this.onSelectedMetrics[selectedReviews] = selectedMetrics;
  }
  onChanges(): void {
    const name = 'name';
    const releaseTime = 'releaseTime';
    const deadline = 'deadline';
    const description = 'description';
    const reviewReleaseTime = 'reviewReleaseTime';
    const reviewDeadline = 'reviewDeadline';
    const commitRecordCount = 'commitRecordCount';
    const type = 'type';
    this.assignment.get(name).valueChanges.subscribe(
      () => {
        this.assignment.get(name).valid ? this.showIsValidById(name) : this.showIsInvalidById(name);
      }
    );

    this.assignment.get(releaseTime).valueChanges.subscribe(
      val => {
        val.length !== 0 ? this.showIsValidById(releaseTime) : this.showIsInvalidById(releaseTime);
        this.assignmentTimeCheck();
      }
    );
    this.assignment.get(deadline).valueChanges.subscribe(
      val => {
        val.length !== 0 ? this.showIsValidById(deadline) : this.showIsInvalidById(deadline);
        this.assignmentTimeCheck();
      }
    );
    this.assignment.get(type).valueChanges.subscribe(
      () => {
        if (this.assignment.get(type).value == 'javac') {
          this.setShowAssessment(false);
        } else {
          this.setShowAssessment(true);
        }
      }
    );

    this.assignment.get(description).valueChanges.subscribe(
      val => {
        val.length !== 0 ? this.showIsValidById(description) : this.showIsInvalidById(description);
      }
    );
    // Peer Review Options
    this.assignment.get(reviewReleaseTime).valueChanges.subscribe(
      val => {
        val.length !== 0 ? this.showIsValidById(reviewReleaseTime) : this.showIsInvalidById(reviewReleaseTime);
        this.assignmentTimeCheck();
      }
    );
    this.assignment.get(reviewDeadline).valueChanges.subscribe(
      val => {
        val.length !== 0 ? this.showIsValidById(reviewDeadline) : this.showIsInvalidById(reviewDeadline);

        this.assignmentTimeCheck();
      }
    );
    this.assignment.get(commitRecordCount).valueChanges.subscribe(
      val => {
        val.length !== 0 ? this.showIsValidById(commitRecordCount) : this.showIsInvalidById(commitRecordCount);
      }
    );
  }

  setShowAssessment(showAssessment: boolean) {
    this.showAssessment = showAssessment;
  }

  getScoreOptions(order: string[], statusScore: Map<string, string>, status: string) {
    let max = 100;
    let sum = 0;
    let options : string[] = [];
    let tempOrder = order;
    let tempStatusScore = statusScore;

    if (statusScore = this.javaStatusScore) {
      sum += Number(statusScore.get('Compile Failure'));
    }

    if (tempOrder.length !== 0) {
      for (let i = 0; i < tempOrder.length; i++) {
        if (tempStatusScore.get(tempOrder[i]) !== undefined) {
          sum += Number(tempStatusScore.get(tempOrder[i]));
        }
      }
    }
    if (tempStatusScore.get(status) !== undefined) {
      sum -= Number(tempStatusScore.get(status));
    }
    max = max - sum;
    for (let i = 0; i <= 100; i++) {
      if (i <= max) {
        options.push(String(i));
      }
    }
    return options;
  }

  selectChangeHandler(type:string, status:string, $event) {
    if (type == 'maven') {
      this.javaStatusScore.set(status, $event.target.value);
      console.log(this.javaStatusScore);
    } else if (type == 'web') {
      this.webStatusScore.set(status, $event.target.value);
      console.log(this.webStatusScore);
    }
  }

  selectedAssignmentGradingMethod(method: string) {
    if (method !== undefined) {
      this.assignment.get('method').setValue(assignmentGradingEnum[method]);
      if (assignmentGradingEnum[method] === assignmentGradingEnum.Auto) {
        this.autoAssignmentStatus.isOpen = true;
        this.peerReviewStatus.isOpen = false;
        const now_time = Date.now() - (new Date().getTimezoneOffset() * 60 * 1000);
        // ReSet Peer Review Form Control
        this.assignment.patchValue({ commitRecordCount: 0 });
        this.assignment.patchValue({ reviewReleaseTime: new Date(now_time).toISOString().slice(0, 17) + '00' });
        this.assignment.patchValue({ reviewDeadline: new Date(now_time).toISOString().slice(0, 17) + '00' });
      } else {
        this.autoAssignmentStatus.isOpen = false;
        this.peerReviewStatus.isOpen = true;
      }
    }
  }

  addReviewMetrics() {
    this.reviewMetricsNums = Array(this.reviewMetricsNums.length + 1).fill(0).map((x, i) => i);
  }

  removeReviewMetrics(index: number) {
    for (index; index < this.reviewMetricsNums.length - 1; index++) {
      this.onSelectedCategory[index] = this.onSelectedCategory[index + 1];
      this.onSelectedMetrics[index] = this.onSelectedMetrics[index + 1];
    }
    this.onSelectedCategory.splice(this.reviewMetricsNums.length - 1, 1);
    this.onSelectedMetrics.splice(this.reviewMetricsNums.length - 1, 1);
    this.reviewMetricsNums.splice(this.reviewMetricsNums.length - 1, 1);
    this.reviewMetricsNums = Array(this.reviewMetricsNums.length).fill(0).map((x, i) => i);
  }

  getAllMetrics() {
    for (const category of this.categories) {
      this.createService.getMetrics(category).subscribe(
        response => {
          category.allMetrics = response['allMetrics'];
        }
      );
    }
  }

  selectedAssignmentType(type: string) {
    if (type !== undefined) {
      this.assignment.get('type').setValue(assignmentTypeEnum[type]);
    }
  }

  showIsValidById(id: string) {
    $('#' + id).addClass('is-valid');
    $('#' + id).removeClass('is-invalid');
  }

  showIsInvalidById(id: string) {
    $('#' + id).removeClass('is-valid');
    $('#' + id).addClass('is-invalid');
  }

  changeToAssignmentPage() {
    this.router.navigate(['./dashboard/assignmentManagement']);
  }

  fileListener($event) {
    this.assignment.controls['file'].setValue($event.target.files[0]);
  }

  assignmentTimeCheck() {
    const releaseTime = 'releaseTime';
    const deadline = 'deadline';
    const reviewReleaseTime = 'reviewReleaseTime';
    const reviewDeadline = 'reviewDeadline';
    // deadline should be after release time
    if (Date.parse(this.assignment.get(deadline).value) < Date.parse(this.assignment.get(releaseTime).value)) {
      this.showIsInvalidById(deadline);
    }
    // review start time should be after deadline
    if (Date.parse(this.assignment.get(reviewReleaseTime).value) < Date.parse(this.assignment.get(deadline).value)) {
      this.showIsInvalidById(reviewReleaseTime);
    }
    // review end time should be after review start time
    if (Date.parse(this.assignment.get(reviewDeadline).value) < Date.parse(this.assignment.get(reviewReleaseTime).value)) {
      this.showIsInvalidById(reviewDeadline);
    }
  }

  public submit() {
    let tempOrder: string[] = [];
    let tempStatusScore: Map<string, string>;
    let orderString = "";
    if (this.assignment.get('type').value == 'maven') {
      tempOrder = this.javaOrder;
      tempStatusScore = this.javaStatusScore;
      orderString = "Compile Failure:" + this.javaStatusScore.get("Compile Failure") + ", ";
    } else if (this.assignment.get('type').value == 'web') {
      tempOrder = this.webOrder;
      tempStatusScore = this.webStatusScore;
    }
    for (let i = 0; i < tempOrder.length; i++) {
      if (tempStatusScore.get(tempOrder[i]) == undefined) {
        orderString = orderString + tempOrder[i] + ':0';
      } else {
        orderString = orderString + tempOrder[i] + ':' + tempStatusScore.get(tempOrder[i]);
      }
      if (i < tempOrder.length - 1) {
        orderString = orderString + ', ';
      }
    }
    this.assignment.get('assOrder').setValue(orderString);
    if (this.assignment.dirty && this.assignment.valid) {
      this.progressModal.show();
      if (!this.peerReviewStatus.isOpen) {
        if (this.assignment.get('type').value == 'maven' || this.assignment.get('type').value == 'web') {
          this.createService.createAssignmentWithOrder(this.assignment).subscribe(
            (response) => {
              this.router.navigate(['./dashboard/assignmentManagement']);
            },
            error => {
              this.errorResponse = error;
              this.errorTitle = 'Create Assignment Error';
              this.progressModal.hide();
            });
        } else {
          this.createService.createAssignment(this.assignment).subscribe(
            (response) => {
              this.router.navigate(['./dashboard/assignmentManagement']);
            },
            error => {
              this.errorResponse = error;
              this.errorTitle = 'Create Assignment Error';
              this.progressModal.hide();
            });
        }
      } else {
        const selectedMetrics = [];
        for (let i = 0; i < this.reviewMetricsNums.length; i++) {
          selectedMetrics[i] = this.categories[this.onSelectedCategory[i]].allMetrics[this.onSelectedMetrics[i]].id;
        }
        this.createService.createPeerReviewAssignment(this.assignment, selectedMetrics).subscribe(
          (response) => {
            this.router.navigate(['./dashboard/assignmentManagement']);
          },
          error => {
            this.errorResponse = error;
            this.errorTitle = 'Create Assignment Error';
            this.progressModal.hide();
          });
      }
    } else {
      return;
    }
  }

  public confirm(order: string[]) {
    let tempOrder = order;
    let orderString: string = "";
    if (this.assignment.get('type').value == 'maven') {
      orderString = 'Compile Failure' + ', ';
    }
    for (let i = 0; i < tempOrder.length; i++) {
      orderString = orderString + tempOrder[i];
      if (i < tempOrder.length - 1) {
        orderString = orderString + ', '
      }
    }
    this.assignment.get('assOrder').setValue(orderString);
    if (this.assignment.get('name').invalid) {
      if (this.assignment.get('type').value == 'maven' || this.assignment.get('type').value == undefined) {
        window.open(environment.SERVER_URL + '/resources/MvnQuickStart.zip');
      }
    } else {
      this.createService.modifyOrder(this.assignment).subscribe(
        (response) => {
          console.log("Success");
          window.open(this.createService.getAssignmentFile(this.assignment.value.name));
        },
        error => {
          this.errorResponse = error;
          this.errorTitle = 'Send Order Error';
        });
    }
  }

  ngOnDestroy() {
    this.javaTabStatus.isOpen = false;
    this.webTabStatus.isOpen = false;
    this.androidTabStatus.isOpen = false;
  }

  
}
