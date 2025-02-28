import {Identity} from "./Identity";

export interface Network {
  network: string;
  subnetworks?: string[];
}

export interface ParamTp {
  tp?: "TpString" | "TpInt";
  maxLen?: number;
  multiline?: boolean;
}
export interface CardanoTemplateParameter {
  name: string;
  description?: string;
  tp?: ParamTp;
  optional?: boolean;
}

export interface CardanoTemplate {
  name: string;
  description: string;
  parameters?: CardanoTemplateParameter[];
}

export interface Contract {
  targetAddress?: string;
  changeSubmitCost?: number;
  targetMintingPolicy?: string;
  submitMintingPolicy?: string;
  votingTokenPolicy?: string;
  votingTokenAsset?: string;
}
export interface Cardano {
  contract?: Contract
}

export interface Registry extends Identity<string> {
  name: string;
  schema: string;
  network: string;
  subnetwork?: string;
  didPrefix?: string;
  proofspaceServiceDid?: string;
  proofspaceNetwork?: string;
  cardano?: Cardano;
  lastChangeDate?: string;
}
