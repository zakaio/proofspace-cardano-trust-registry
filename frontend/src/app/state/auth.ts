import {User} from "../../domain/User";
import {createAsyncThunk, createSlice, PayloadAction} from "@reduxjs/toolkit";

export interface AuthState {
  isLoading?: boolean;
  isQrAuthLoading?: boolean;
  user?: User;
  qrUrl?: string;
  when?: number;
  error?: string;
}

const initialState: AuthState = {
  user: {email: 'test'},
  isLoading: false
};

export const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setLoading: (state) => {
      state.isLoading = true;
    },
    setQrAuthLoading: (state) => {
      state.isQrAuthLoading = true;
    },
    setUser: (state, action: PayloadAction<User>) => {
      state.user = action.payload;
      state.isLoading = false;
    },
    setQrData: (state, action: PayloadAction<{qrUrl: string, when: number}>) => {
      state.qrUrl = action.payload.qrUrl;
      state.when = action.payload.when;
      state.isQrAuthLoading = false;
    },
    setError: (state, action: PayloadAction<string>) => {
      state.error = action.payload;
    },
    dropUser: (state) => {
      state.user = undefined;
      state.isLoading = false;
    },
  }
});

const {setLoading, setQrAuthLoading, setUser, setQrData, setError, dropUser} = authSlice.actions;

/*export const status = createAsyncThunk('status', async (_, {dispatch}) => {
  dispatch(setLoading());
  const user = await authApi.getStatus();
  console.log('user is', user);
  if (user) {
    if (user.signInRecord) {
      const tkn = await authApi.getSignUpToken();
      console.log('sign up token is', tkn);
      setSignUpToken(tkn.token);
    }
    dispatch(setUser(user));
  } else {
    dispatch(logout());
  }
});*/

/*export const logout = createAsyncThunk('logout', async (_, {dispatch}) => {
  dropToken();
  dropSignUpToken();
  try {
    await httpPostEmpty(`${customerPrefix()}/logout`);
  } catch (e) {
    console.log('Unable to log out', e);
  }
  dispatch(dropUser());
});*/
