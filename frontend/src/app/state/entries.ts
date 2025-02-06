import {createAsyncThunk, createSlice, PayloadAction} from "@reduxjs/toolkit";
import {ItemsState} from "./base";
import {Entry} from "../../domain/Entry";
import {entriesApi} from "../api/API";

let itemsPerPage = 9;
let currentPage = 1;
let filter = '';

const initialState: ItemsState<Entry> = {
  items: [],
  itemsTotal: 0,
  currentPage: 1,
  itemsPerPage: 10,
  filter: ''
};

export const entrySlice = createSlice({
  name: 'entry',
  initialState,
  reducers: {
    setLoading: (state) => {
      state.isLoading = true;
    },
    setEntries: (state, action: PayloadAction<ItemsState<Entry>>) => {
      state.items = action.payload.items;
      state.itemsTotal = action.payload.itemsTotal;
      state.itemsPerPage = action.payload.itemsPerPage;
      state.currentPage = action.payload.currentPage;
      state.filter = action.payload.filter;
      state.isLoading = false;
    },
  }
});

const {setLoading, setEntries} = entrySlice.actions;

export const getEntries = createAsyncThunk(
  'get-entries',
  async (arg: {registryId: string, currentPage: number, itemsPerPage: number, filter: string}, {dispatch}) => {
    dispatch(setLoading());
    currentPage = arg.currentPage;
    itemsPerPage = arg.itemsPerPage;
    filter = arg.filter;
    let offset = (currentPage - 1) * itemsPerPage;
    if (offset < 0) {
      offset = 0;
    }

    const entries = await entriesApi.list(
      {parent: arg.registryId, range: {limit: itemsPerPage, offset}, commonFilter: filter}
    );

    dispatch(setEntries({ ...entries, currentPage, itemsPerPage, filter}));
  }
);

export const proposeChanges = createAsyncThunk(
  'propose-changes',
  async (arg: {registryId: string, added: string[], removed: string[]}, {dispatch}) => {
    dispatch(setLoading());
    await entriesApi.proposeChanges(arg.registryId, arg.added, arg.removed);
    dispatch(getEntries({registryId: arg.registryId, currentPage, itemsPerPage, filter}));
  }
);
