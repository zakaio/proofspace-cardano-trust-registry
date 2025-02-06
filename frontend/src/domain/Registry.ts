import {Identity} from "./Identity";

export interface Registry extends Identity<string> {
  name: string;
  network: string;
  subnetwork: string;
  didPrefix: string;
  lastChangeDate: string;
}
