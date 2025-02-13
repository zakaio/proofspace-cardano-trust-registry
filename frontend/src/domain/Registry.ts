import {Identity} from "./Identity";

export interface Network {
  network: string;
  subnetworks?: string[];
}

export interface Registry extends Identity<string> {
  name: string;
  network: string;
  subnetwork: string;
  didPrefix: string;
  lastChangeDate: string;
}
