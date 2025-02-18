import {Identity} from "./Identity";

export interface ProposedChange extends Identity<string> {
  registryId: string;
  addedDids: string[];
  removedDids: string[];
  approved: boolean;
  changeDate: string;
  transactionId: string;
}
