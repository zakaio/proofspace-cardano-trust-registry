import {createSlice, PayloadAction} from "@reduxjs/toolkit";
import {User} from "../../domain/User";

interface AppParams {
  deviceCode: string;
  redirect?: string;
}

const appParams: AppParams = {
  deviceCode: '',
}

export interface AppState {
  qrUrl: string;
  //qrExpirationPeriod: number;
  qrExpiresAt: number;
}

const initialState: AppState = {
  qrUrl: '',
  //qrExpirationPeriod: 30 * 60 * 1000,
  qrExpiresAt: Date.now() + 30 * 60 * 1000
};

export const appSlice = createSlice({
  name: 'app',
  initialState,
  reducers: {
    setParams: (state, action: PayloadAction<{qrUrl: string, qrExpiresAt: number}>) => {
      state.qrUrl = action.payload.qrUrl;
      state.qrExpiresAt = action.payload.qrExpiresAt;
    },
  }
});
