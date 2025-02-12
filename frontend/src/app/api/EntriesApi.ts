import {httpDelete, httpGetJSON, httpPostEmpty, httpPostJSON, httpPutFile, httpPutJSON, signUpToken} from "./Fetch";
import {appConfig} from "../cfg/config";
import {conditionToQueryString} from "./QueryString";
import {Converter} from "./Converter";
import {ChangeOptions, GetCondition, ListItemApi} from "./ListItemApi";
import {Change, Entry} from "../../domain/Entry";

interface ChangeApiObject {
  changeId: string;
  status: any;
  changeDate: string;
}

interface EntryApiObject {
  did: string;
  status: any;
  acceptedChange: ChangeApiObject;
  proposedChange: ChangeApiObject;
}

const FAKE: EntryApiObject[] = [
  {did: '123', status: 'some', proposedChange: {changeId: '1', status: '', changeDate: '123'}, acceptedChange: {changeId: '0', status: 's', changeDate: ''}},
  {did: '234', status: 'ret', proposedChange: {changeId: '1', status: '', changeDate: '123'}, acceptedChange: {changeId: '0', status: 's', changeDate: ''}},
  {did: '345', status: 'poi', proposedChange: {changeId: '1', status: '', changeDate: '123'}, acceptedChange: {changeId: '0', status: 's', changeDate: ''}},
  {did: '567', status: 'bebe', proposedChange: {changeId: '1', status: '', changeDate: '123'}, acceptedChange: {changeId: '0', status: 's', changeDate: ''}},
  {did: '678', status: 'ahaha', proposedChange: {changeId: '1', status: '', changeDate: '123'}, acceptedChange: {changeId: '0', status: 's', changeDate: ''}},
];

class ChangeConverter extends Converter<Change, ChangeApiObject> {
  apiObjectToItem(obj: ChangeApiObject): Change {
    return {...obj, identity: obj.changeId};
  }

  itemToApiObject(item: Change): ChangeApiObject {
    return {...item, changeId: item.identity};
  }
}

class EntriesConverter extends Converter<Entry, EntryApiObject> {
  private changeConverter = new ChangeConverter();
  apiObjectToItem(obj: EntryApiObject): Entry {
    const item: Entry = {
      ...obj,
      identity: obj.did,
      acceptedChange: this.changeConverter.apiObjectToItem(obj.acceptedChange),
      proposedChange: this.changeConverter.apiObjectToItem(obj.proposedChange)
    };
    return item;
  }

  itemToApiObject(item: Entry): EntryApiObject {
    const res: EntryApiObject = {
      ...item, did:
      item.identity,
      acceptedChange: this.changeConverter.itemToApiObject(item.acceptedChange),
      proposedChange: this.changeConverter.itemToApiObject(item.proposedChange)
    };
    return res;
  }
}

const converterObject = {
  identity: 'did',
};

export class EntriesApi extends ListItemApi<Entry, EntryApiObject, string> {
  constructor() {
    super(new EntriesConverter());
  }

  async proposeChanges(registryId: string, added: string[], removed: string[]) {
    return await httpPostJSON(
      `${appConfig().BACKEND}/trust-registry/${registryId}/change`,
      {registryId, addedDids: added, removedDids: removed}
    )
  }

  async approveChanges(registryId: string, changeId: string) {
    return await httpPostJSON(
      `${appConfig().BACKEND}/trust-registry/${registryId}/change/${changeId}/approve`,
      {}
    )
  }

  async rejectChanges(registryId: string, changeId: string) {
    return await httpPostJSON(
      `${appConfig().BACKEND}/trust-registry/${registryId}/change/${changeId}/reject`,
      {}
    )
  }

  protected async fetchOne(condition?: GetCondition<string>): Promise<EntryApiObject> {
    const id = condition && condition.identity ? condition.identity : '';
    return await httpGetJSON(`${appConfig().BACKEND}/trust-registry/${encodeURI(id as string)}`);
  }

  protected async fetchList(condition?: GetCondition<string>) {
    // return {itemsTotal: 32, items: FAKE};
    const resp = await httpGetJSON(
      `${appConfig().BACKEND}/trust-registry/${condition?.parent || ''}/entries${conditionToQueryString(condition, converterObject)}`
    );
    return resp;
  }

  protected async fetchCreate(item: EntryApiObject): Promise<EntryApiObject> {
    return item;
    /*const d: any = {...item};
    delete d.id;
    const created = await httpPostJSON(`${appConfig().BACKEND}/project`, d);
    return created;*/
  }

  protected async fetchSave(item: EntryApiObject, opt?: ChangeOptions<string>): Promise<void> {
    console.log('i call save');
    return;
    /*const prevName = opt ? opt.prevIdentity : item.did;
    await httpPutJSON(`${customerPrefix()}/project/${encodeURI(prevName as string)}`, item);*/
  }

  protected async fetchDelete(item: EntryApiObject): Promise<void> {
    // await httpDelete(`${appConfig().BACKEND}/project/${encodeURI(item.did)}`);
  }

}
