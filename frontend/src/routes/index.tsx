import {FC, lazy} from 'react';
import {createBrowserRouter, Navigate, Outlet, redirect} from 'react-router-dom';
import {User} from "../domain/User";
import {AuthPath, MainPath} from "../app/cfg/RoutePath";
import Loadable from "../components/LoadableComponent";
import Main from "../components/Main";
import {queryToObject} from "../utils/UrlUtil";
import {find} from "lodash";
import {paramsStorage} from "../domain/ParamStorage";

//const SignUpForm = Loadable(lazy(() => import("../components/auth/SignUpForm")));
const Auth = Loadable(lazy(() => import("../components/Auth")));

const RegistryList = Loadable(lazy(() => import('../components/RegistryList')));
const EntriesLayout = Loadable(lazy(() => import('../components/EntriesLayout')));
const Denied = Loadable(lazy(() => import('../components/Dinied')));

export const createRoutes = (root: string, user?: User) => {

  const authGuard = async () => {
    if (!user) {
      console.log('Guard works and redirects to login');
      return redirect(`${root}${AuthPath.LOGIN}`);
    }
    /*const serviceDid = paramsStorage.get();
    if (serviceDid) {
      console.log('we have serviceDid', serviceDid);
      const record = find(user.serviceRecords, (r) => r.serviceDid === serviceDid);
      if (!record) {
        console.log('so, we have no access', user.serviceRecords);
        paramsStorage.drop();
        return redirect(`${root}${MainPath.DENIED}`);
      } else {
        const url = await generateServiceAuthUrl(record.endpointUrl, serviceDid);
        console.log('we have access url', url);
        // return redirect(url);
        window.open(url, '_self');
      }
    }*/
    return null;
  };

  const isLoggedIn = async () => {
    if (user) {
      return redirect(`${root}${MainPath.ALL}`);
    }

    return null;
  };

  return createBrowserRouter([
    {
      path: `${root}`,
      element: <Main><Outlet/></Main>,
      children: [
        {
          path: AuthPath.LOGIN,
          loader: isLoggedIn,
          element: <Auth />
        },
        {
          path: MainPath.DENIED,
          element: <Denied />,
        },
        {
          path: '',
          element: <Outlet/>,
          loader: authGuard,
          children: [
            {path: '', index: true, element: <RegistryList />},
            {
              path: `${MainPath.ALL}`,
              element: <RegistryList />,
            },
            {
              path: `${MainPath.REGISTRY}/:id`,
              element: <Outlet />,
              children: [
                {path: ':section', element: (<EntriesLayout/>)},
                {path: '', element: (<EntriesLayout/>)}
              ]
            },
          ]
        },
        {path: '*', element: <Navigate to="" />}
      ]
    }
  ]);
}
