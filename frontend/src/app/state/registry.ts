import {createAsyncThunk, createSlice, PayloadAction} from "@reduxjs/toolkit";
import {ItemsState} from "./base";
import {entriesApi, registryApi} from "../api/API";
import {Network, Registry} from "../../domain/Registry";

let itemsPerPage = 9;
let currentPage = 1;
let filter = '';

interface RegistryState extends ItemsState<Registry>{
  networks: Network[];
}

const initialState: RegistryState = {
  networks: [],
  items: [],
  itemsTotal: 0,
  currentPage: 1,
  itemsPerPage: 10,
  filter: ''
};

export const registrySlice = createSlice({
  name: 'registry',
  initialState,
  reducers: {
    setLoading: (state) => {
      state.isLoading = true;
    },
    setRegistries: (state, action: PayloadAction<ItemsState<Registry>>) => {
      state.items = action.payload.items || [];
      state.itemsTotal = action.payload.itemsTotal;
      state.itemsPerPage = action.payload.itemsPerPage;
      state.currentPage = action.payload.currentPage;
      state.filter = action.payload.filter;
      state.isLoading = false;
    },
    setNetworks: (state, action: PayloadAction<Network[]>) => {
      state.networks = action.payload;
    }
  }
});

const {setLoading, setRegistries, setNetworks} = registrySlice.actions;

export const getRegistries = createAsyncThunk(
  'get-registries',
  async (arg: {currentPage: number, itemsPerPage: number, filter: string}, {dispatch}) => {
    dispatch(setLoading());

    const netResp = await registryApi.getNetworks();
    dispatch(setNetworks(netResp.items || []));

    currentPage = arg.currentPage;
    itemsPerPage = arg.itemsPerPage;
    filter = arg.filter;
    let offset = (currentPage - 1) * itemsPerPage;
    if (offset < 0) {
      offset = 0;
    }

    const res = await registryApi.list({range: {limit: itemsPerPage, offset}, commonFilter: filter});
    dispatch(setRegistries({ ...res, currentPage, itemsPerPage, filter}));
  }
);

export const saveRegistry = createAsyncThunk(
  'save-registry',
  async (arg: {item: Registry}, {dispatch}) => {
    const {item} = arg;
    dispatch(setLoading());
    if (!item.identity) {
      await registryApi.create(item);
    } else {
      await registryApi.save(item);
    }
    dispatch(getRegistries({currentPage, itemsPerPage, filter}));
  }
);

export const deleteRegistry = createAsyncThunk(
  'delete-registry',
  async (arg: {item: Registry}, {dispatch}) => {
    const {item} = arg;
    dispatch(setLoading());
    await registryApi.delete(item);
    dispatch(getRegistries({currentPage, itemsPerPage, filter}));
  }
);
