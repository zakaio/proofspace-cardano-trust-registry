import React from 'react';
import './App.css';
import {useAppDispatch, useAppSelector, useDidMount} from "./app/hooks";
import {LoaderView} from "./components/LoaderView";
import {createRoutes} from "./routes";
import {appConfig} from "./app/cfg/config";
import {RouterProvider} from "react-router-dom";
import {queryToObject} from "./utils/UrlUtil";
import {paramsStorage} from "./domain/ParamStorage";

const App = () => {
  const dispatch = useAppDispatch();

  const user = useAppSelector((state) => state.auth.user);
  const isLoading = useAppSelector((state) => state.auth.isLoading);

  const params = queryToObject(window.location.search);
  const serviceDid = params.serviceDid;
  if (serviceDid) {
    paramsStorage.store(serviceDid);
  }

  useDidMount(() => {
    // dispatch(status());
    // dispatch(initCatalogue());
  });

  if (isLoading) {
    return <LoaderView />;
  }

  return (
    <RouterProvider router={createRoutes(appConfig().ROOT, user)} fallbackElement={<LoaderView />} />
  );
}

export default App;
