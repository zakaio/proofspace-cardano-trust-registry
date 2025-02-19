import {Identity} from "./Identity";

export interface Network {
  network: string;
  subnetworks?: string[];
}

export interface Registry extends Identity<string> {
  name: string;
  schema: string;
  network: string;
  subnetwork?: string;
  didPrefix: string;
  proofspaceServiceDid?: string;
  proofspaceNetwork?: string;
  createTargetAddress?: string;
  createSubmitCost?: number;
  changeTargetAddress?: string;
  changeSubmitCost?: number;
  targetMintingPolicy?: string;
  changeSubmitMintingPolicy?: string;
  lastChangeDate?: string;
}
