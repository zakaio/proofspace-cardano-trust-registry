import {FC} from "react";
import {AppBar, Avatar, Box, Button, createTheme, Tab, Tabs, ThemeProvider, Toolbar, Tooltip} from "@mui/material";
import logo from '../PSLogoSmall.png';
import {MainPath} from "../app/cfg/RoutePath";
import KeyIcon from '@mui/icons-material/Key';
import MiscellaneousServicesIcon from '@mui/icons-material/MiscellaneousServices';
import {Link, useLocation} from "react-router-dom";
import {appConfig, createLink} from "../app/cfg/config";
import {useAppDispatch, useAppSelector} from "../app/hooks";
import LogoutIcon from "@mui/icons-material/Logout";
import {green} from "@mui/material/colors";
import {localize} from "../app/cfg/Language";

interface Props {
  children: any;
}

const theme = createTheme({
  palette: {
    primary: {main: '#5D4AEE'},
    secondary: {main: '#f8f8f8'},
    action: {
      disabledOpacity: 0.1,
    }
  },
  typography: {
    fontFamily: 'inherit',
    button: {
      textTransform: 'none',
      fontSize: 16
    }
  }
});

const Main: FC<Props> = ({children}) => {
  const user = useAppSelector((state) => state.auth.user);

  const dispatch = useAppDispatch();

  // const onLogout = () => dispatch(logout());

  const location = useLocation();

  const value = location.pathname.split(appConfig().ROOT)[1] || MainPath.ALL;

  return (
    <ThemeProvider theme={theme}>
      <Box height="100vh">
        {user && value !== MainPath.DENIED && (
          <AppBar color="secondary" position="static">
              <Toolbar>
                <div style={{paddingTop: 8, paddingRight: 64}}>
                  <img src={logo} alt="ProofSpace logo" />
                </div>
                <div style={{flexGrow: 1}} />
                {/*}
                <div style={{paddingRight: 16}}>
                  <Tooltip title={user.email}>
                    <Avatar sx={{ bgcolor: green[500] }}>
                      {user.email[0].toUpperCase()}
                    </Avatar>
                  </Tooltip>
                </div>
                <div>
                  <Button onClick={onLogout} startIcon={<LogoutIcon/>} variant={'outlined'}>{localize('LOGOUT')}</Button>
                </div>
              {*/}
              </Toolbar>
          </AppBar>
        )}
        {user ? (
          <Box component="main" sx={{ p: 3 }}>
            {children}
          </Box>
        ) : (
          <Box component="main">
            {children}
          </Box>
        )}
      </Box>
    </ThemeProvider>
  );
};

export default Main;
