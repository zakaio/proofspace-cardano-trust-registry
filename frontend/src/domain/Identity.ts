export interface Identity<T = string | number> {
  identity: T;
  [key: string]: any;
}
