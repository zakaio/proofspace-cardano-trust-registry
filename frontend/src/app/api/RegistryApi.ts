import {httpDelete, httpGetJSON, httpPostEmpty, httpPostJSON, httpPutFile, httpPutJSON, signUpToken} from "./Fetch";
import {appConfig} from "../cfg/config";
import {conditionToQueryString} from "./QueryString";
import {Converter} from "./Converter";
import {ChangeOptions, GetCondition, ListItemApi} from "./ListItemApi";
import {Registry} from "../../domain/Registry";

interface RegistryApiObject {
  id: string;
  name: string;
  network: string;
  subnetwork: string;
  didPrefix: string;
  lastChangeDate: string
}

const FAKE: RegistryApiObject [] = [
  {id: '1', name: 'one', network: 'main', subnetwork: 'test', didPrefix: 'aaaa_', lastChangeDate: ''},
  {id: '2', name: 'two', network: 'main', subnetwork: 'test', didPrefix: 'bbbb_', lastChangeDate: ''},
  {id: '3', name: 'reee', network: 'main', subnetwork: 'test2', didPrefix: 'ccc_', lastChangeDate: ''}
];

class RegistryConverter extends Converter<Registry, RegistryApiObject> {
  apiObjectToItem(obj: RegistryApiObject): Registry {
    const item: Registry = {...obj, identity: obj.id};
    return item;
  }

  itemToApiObject(item: Registry): RegistryApiObject {
    const res: RegistryApiObject = {...item, id: item.identity};
    return res;
  }
}

const converterObject = {
  identity: 'id',
};

export class RegistryApi extends ListItemApi<Registry, RegistryApiObject> {
  constructor() {
    super(new RegistryConverter());
  }

  protected async fetchOne(condition?: GetCondition): Promise<RegistryApiObject> {
    const id = condition && condition.identity ? condition.identity : '';
    return await httpGetJSON(`${appConfig().BACKEND}/trust-registry/${encodeURI(id as string)}`);
  }

  protected async fetchList(condition?: GetCondition) {
    return {itemsTotal: 35, items: FAKE};
    const resp = await httpGetJSON(
      `${appConfig().BACKEND}/trust-registry${conditionToQueryString(condition, converterObject)}`
    );
    return resp;
  }

  protected async fetchCreate(item: RegistryApiObject): Promise<RegistryApiObject> {
    return item;
    const d: any = {...item};
    delete d.id;
    const created = await httpPostJSON(`${appConfig().BACKEND}/trust-registry`, d);
    return created;
  }

  protected async fetchSave(item: RegistryApiObject, opt?: ChangeOptions): Promise<void> {
    console.log('i call save');
    const prevName = opt ? opt.prevIdentity : item.id;
    await httpPostJSON(`${appConfig().BACKEND}/trust-registry/${encodeURI(prevName as string)}`, item);
  }

  protected async fetchDelete(item: RegistryApiObject): Promise<void> {
    await httpDelete(`${appConfig().BACKEND}/trust-registry/${encodeURI(item.id)}`);
  }

}
