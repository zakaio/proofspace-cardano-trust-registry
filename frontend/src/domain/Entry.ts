import {Identity} from "./Identity";

export interface Change extends Identity<string> {
  status: any;
  changeDate: string;
}

export interface Entry extends Identity<string> {
  status: any;
  acceptedChange: Change;
  proposedChange: Change;
}
