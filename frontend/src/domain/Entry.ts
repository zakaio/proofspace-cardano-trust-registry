import {Identity} from "./Identity";

export interface EntryStatus {
  type: string;
}

export interface ChangeStatus {
  type: string;
}

export interface Change extends Identity<string> {
  status: ChangeStatus;
  changeDate: string;
}

export interface Entry extends Identity<string> {
  status: EntryStatus;
  acceptedChange?: Change;
  proposedChange?: Change;
}
