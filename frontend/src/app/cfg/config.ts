class ConfigData {
  readonly ZAKA_BACKEND: string = process.env.REACT_APP_ZAKA_BACKEND || 'https://test.proofspace.id/zaka';
  readonly BACKEND: string = process.env.REACT_APP_BACKEND || 'https://test.proofspace.id/trustregistry';
  readonly ROOT: string = process.env.REACT_APP_ROOT_PATH || '/';
  readonly NETWORK: string = process.env.REACT_APP_NETWORK || 'test';
  // readonly FRONT_HOST: string = process.env.REACT_APP_FRONT_HOST || 'test.proofspace.id';
  readonly SERVICE_URL: string = process.env.REACT_APP_SERVICE_URL || 'https://test.proofspace.id/zaka-agents-container-frontend';
  readonly DEEP_LINK_PROTOCOL: string= process.env.REACT_APP_DEEP_LINK_PROTOCOL || 'zakaio';
}

// export const deepLinkPrefix = () => `${appConfig().DEEP_LINK_PROTOCOL}://${appConfig().FRONT_HOST}/native/execute/`;

export const sseUri = (code: string) =>
  `${appConfig().BACKEND}/auth-sse?network=${appConfig().NETWORK}&code=${code}`;


let _appConfig: ConfigData = new ConfigData();

export const appConfig = () => {
  if (_appConfig === undefined) {
    throw new Error('Attempt to use appConfig which is not defined');
  } else {
    return _appConfig;
  }
};

export const createLink = (value: string) => `${appConfig().ROOT}${value}`;
