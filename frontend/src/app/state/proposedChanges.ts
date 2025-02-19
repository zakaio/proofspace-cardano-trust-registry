import {createAsyncThunk, createSlice, PayloadAction} from "@reduxjs/toolkit";
import {ItemsState} from "./base";
import {proposedChangesApi} from "../api/API";
import {ProposedChange} from "../../domain/ProposedChange";
import {reloadEntries} from "./entries";

let itemsPerPage = 9;
let currentPage = 1;
let filter = '';

const initialState: ItemsState<ProposedChange> = {
  items: [],
  itemsTotal: 0,
  currentPage: 1,
  itemsPerPage: 10,
  filter: ''
};

export const changesSlice = createSlice({
  name: 'change',
  initialState,
  reducers: {
    setLoading: (state) => {
      state.isLoading = true;
    },
    setChanges: (state, action: PayloadAction<ItemsState<ProposedChange>>) => {
      state.items = action.payload.items;
      state.itemsTotal = action.payload.itemsTotal;
      state.itemsPerPage = action.payload.itemsPerPage;
      state.currentPage = action.payload.currentPage;
      state.filter = action.payload.filter;
      state.isLoading = false;
    },
  }
});

const {setLoading, setChanges} = changesSlice.actions;

export const getChanges = createAsyncThunk(
  'get-changes',
  async (arg: {registryId: string, currentPage: number, itemsPerPage: number, filter: string}, {dispatch}) => {
    dispatch(setLoading());
    currentPage = arg.currentPage;
    itemsPerPage = arg.itemsPerPage;
    filter = arg.filter;
    let offset = (currentPage - 1) * itemsPerPage;
    if (offset < 0) {
      offset = 0;
    }

    const entries = await proposedChangesApi.list(
      {parent: arg.registryId, range: {limit: itemsPerPage, offset}, commonFilter: filter}
    );

    dispatch(setChanges({ ...entries, currentPage, itemsPerPage, filter}));
  }
);

export const proposeChanges = createAsyncThunk(
  'propose-changes',
  async (arg: {registryId: string, added: string[], removed: string[]}, {dispatch}) => {
    dispatch(setLoading());
    await proposedChangesApi.proposeChanges(arg.registryId, arg.added, arg.removed);
    dispatch(getChanges({registryId: arg.registryId, currentPage, itemsPerPage, filter}));
    dispatch(reloadEntries({registryId: arg.registryId}));
  }
);

export const approveChanges = createAsyncThunk(
  'approve-changes',
  async (arg: {registryId: string, changeId: string}, {dispatch}) => {
    dispatch(setLoading());
    await proposedChangesApi.approveChanges(arg.registryId, arg.changeId);
    dispatch(getChanges({registryId: arg.registryId, currentPage, itemsPerPage, filter}));
  }
);

export const rejectChanges = createAsyncThunk(
  'approve-changes',
  async (arg: {registryId: string, changeId: string}, {dispatch}) => {
    dispatch(setLoading());
    await proposedChangesApi.rejectChanges(arg.registryId, arg.changeId);
    dispatch(getChanges({registryId: arg.registryId, currentPage, itemsPerPage, filter}));
  }
);
