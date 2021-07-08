import { Component, OnInit, ViewChild, SystemJsNgModuleLoader } from '@angular/core';
import { ModalDirective } from 'ngx-bootstrap/modal';
import { Router } from '@angular/router';
import { AssignmentManagementService } from './assignment-management.service';
import { FormBuilder, Validators, FormGroup } from '@angular/forms';
import { TimeService } from '../../../services/time.service'
import { HttpErrorResponse } from '@angular/common/http';

import { environment } from '../../../../environments/environment';
import * as ClassicEditor from '@ckeditor/ckeditor5-build-classic';

@Component({
  selector: 'app-assignment-management',
  templateUrl: './assignment-management.component.html',
  styleUrls: ['../create-assignment/create-assignment.component.scss']
})
export class AssignmentManagementComponent implements OnInit {
  @ViewChild('editModal', { static: true }) public editModal: ModalDirective;
  @ViewChild('deleteModal', { static: true }) public deleteModal: ModalDirective;
  assignments: Array<any>;
  assignmentName: string;
  assignmentForm: FormGroup;
  assignmentOrder: string;

  errorResponse: HttpErrorResponse;
  errorTitle: string;

  max: number = 100;
  showWarning: boolean;
  dynamic: number = 0;
  type: string = 'Waiting';
  isDeleteProgress = false;
  showAssessment: boolean = false;

  order = [];
  statusScore = new Map([["Compile Failure", "0"]]);

  public Editor = ClassicEditor;
  public editorConfig = {
    placeholder: 'Write the assignment description in here!',
    ckfinder: {
      // Upload the images to the server using the CKFinder QuickUpload command.
      uploadUrl: environment.SERVER_URL + `/webapi/assignment/uploadImage`
    }
  };

  constructor(private assignmentService: AssignmentManagementService, private router: Router,
    private fb: FormBuilder, private timeService: TimeService) { }

  ngOnInit() {
    this.getAllAssignments();
    const now_time = Date.now() - (new Date().getTimezoneOffset() * 60 * 1000);
    this.assignmentForm = this.fb.group({
      name: [''],
      releaseTime: [, Validators.required],
      deadline: [, Validators.required],
      description: ['', Validators.required],
      file: [],
      rememberMe: [true],
      order: ['']
    });

    this.onChange();
  }

  onChange() {
    const releaseTime = 'releaseTime';
    const deadline = 'deadline';
    const description = 'description';

    this.assignmentForm.get(releaseTime).valueChanges.subscribe(
      val => {
        val.length !== 0 ? this.showIsValidById(releaseTime) : this.hideIsInvalidById(releaseTime);
      }
    );
    this.assignmentForm.get(deadline).valueChanges.subscribe(
      val => {
        val.length !== 0 ? this.showIsValidById(deadline) : this.hideIsInvalidById(deadline);
      }
    );

    this.assignmentForm.get(description).valueChanges.subscribe(
      val => {
        val.length !== 0 ? this.showIsValidById(description) : this.hideIsInvalidById(description);
      }
    );
  }

  updateTestCase($event) {
    if ($event.target.files) {
      this.assignmentForm.get('file').setValue($event.target.files[0]);
    }
  }

  selectChangeHandler(status:string, $event) {
    this.statusScore.set(status, $event.target.value);
  }

  setShowAssessment(show: boolean) {
    if (show == true) {
      this.showAssessment = true;
    } else {
      this.showAssessment = false;
    }
  }

  getAllAssignments() {
    this.assignmentService.getAllAssignments().subscribe(response => {
      this.assignments = response.allAssignments;
      for (const i in this.assignments) {
        if (i) {
          this.assignments[i].createTime = this.timeService.getUTCTime(this.assignments[i].createTime);
          this.assignments[i].releaseTime = this.timeService.getUTCTime(this.assignments[i].releaseTime);
          this.assignments[i].deadline = this.timeService.getUTCTime(this.assignments[i].deadline);
        }
      }
    });
  }

  getAssignmentOrder() {
    this.order = [];
    this.statusScore = new Map();
    this.assignmentService.getAssignmentOrder(this.assignmentName).subscribe(
      response => {
        this.assignmentOrder = response.orders;
        var splited = this.assignmentOrder.split(', ');
        for (let i = 0; i < splited.length; i++) {
          var statusScore = splited[i].split(':');
          if (statusScore.length > 1) {
            this.setShowAssessment(true);
          }
          if (statusScore[0] == 'COMPILE_FAILURE') {
            this.statusScore.set("Compile Failure", statusScore[1]);
            this.order.push("Compile Failure");
          } else if (statusScore[0] == 'UNIT_TEST_FAILURE') {
            this.statusScore.set("Unit Test Failure", statusScore[1]);
            this.order.push("Unit Test Failure");
          } else if (statusScore[0] == 'CHECKSTYLE_FAILURE') {
            this.statusScore.set("Coding Style Failure", statusScore[1]);
            this.order.push("Coding Style Failure");
          }  else if (statusScore[0] == 'WEB_HTMLHINT_FAILURE') {
            this.statusScore.set("HTML Failure", statusScore[1]);
            this.order.push("HTML Failure");
          } else if (statusScore[0] == 'WEB_STYLELINT_FAILURE') {
            this.statusScore.set("CSS Failure", statusScore[1]);
            this.order.push("CSS Failure");
          } else if (statusScore[0] == 'WEB_ESLINT_FAILURE') {
            this.statusScore.set("JavaScript Failure", statusScore[1]);
            this.order.push("JavaScript Failure");
          }
        }
      }
    )
  }

  getScoreOptions(status:string) {
    let max = 100;
    let sum = 0;
    let options : string[] = [];
    if (this.order.length !== 0) {
      for (let i = 0; i < this.order.length; i++) {
        if (this.statusScore.get(this.order[i]) !== undefined) {
          sum += Number(this.statusScore.get(this.order[i]));
        }
      }
    }
    if (this.statusScore.get(status) !== undefined) {
      sum -= Number(this.statusScore.get(status));
    }
    max = max - sum;
    for (let i = 0; i <= 100; i++) {
      if (i <= max) {
        options.push(String(i));
      }
    }
    return options;
  }

  deleteAssignment() {
    this.isDeleteProgress = true;

    this.assignmentService.deleteAssignment(this.assignmentName).subscribe(
      response => {
        this.deleteModal.hide();
        this.getAllAssignments();
        this.isDeleteProgress = false;
      },
      error => {
        this.errorTitle = 'Delete Assignment Error';
        this.deleteModal.hide();
        this.errorResponse = error;
      });
  }

  showIsValidById(id: string) {
    $('#' + id).addClass('is-valid');
    $('#' + id).removeClass('is-invalid');
  }

  hideIsInvalidById(id: string) {
    $('#' + id).removeClass('is-valid');
    $('#' + id).addClass('is-invalid');
  }

  setSelectAssignment(assignment: any) {
    if (assignment) {
      this.assignmentName = assignment.name;
      this.assignmentForm.get('description').setValue(assignment.description);
      this.assignmentForm.get('releaseTime').setValue(this.timeService.getUTCTime(assignment.releaseTime).toISOString().slice(0, 17) + '00');
      this.assignmentForm.get('deadline').setValue(this.timeService.getUTCTime(assignment.deadline).toISOString().slice(0, 17) + '00');
    }
  }

  editAssignment() {
    let orderString = "";
    for (let i = 0; i < this.order.length; i++) {
      orderString = orderString + this.order[i] + ':' + this.statusScore.get(this.order[i]);
      if (i !== this.order.length - 1) {
        orderString = orderString + ", ";
      }
    }
    this.assignmentForm.get('order').setValue(orderString);
    if (this.assignmentForm.valid) {
      this.assignmentForm.get('name').setValue(this.assignmentName);
      this.assignmentService.editAssignment(this.assignmentForm).subscribe(
        response => {
          this.editModal.hide();
          this.getAllAssignments();
        },
        error => {
          this.errorTitle = 'Edit Assignment Error';
          this.editModal.hide();
          this.errorResponse = error;
        });
    }
    this.setShowAssessment(false);
  }
}