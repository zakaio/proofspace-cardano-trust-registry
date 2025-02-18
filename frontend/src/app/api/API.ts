import {AuthApi} from "./AuthApi";
import {RegistryApi} from "./RegistryApi";
import {EntriesApi} from "./EntriesApi";
import {ProposedChangesApi} from "./ProposedChangesApi";

export const authApi = new AuthApi();

export const registryApi = new RegistryApi();

export const entriesApi = new EntriesApi();

export const proposedChangesApi = new ProposedChangesApi();
