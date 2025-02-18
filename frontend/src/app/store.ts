import { configureStore } from '@reduxjs/toolkit'
import {authSlice} from "./state/auth";
import {appSlice} from "./state/app";
import {registrySlice} from "./state/registry";
import {entrySlice} from "./state/entries";
import {changesSlice} from "./state/proposedChanges";

export const store = configureStore({
  reducer: {
    app: appSlice.reducer,
    auth: authSlice.reducer,
    registry: registrySlice.reducer,
    entry: entrySlice.reducer,
    changes: changesSlice.reducer
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
