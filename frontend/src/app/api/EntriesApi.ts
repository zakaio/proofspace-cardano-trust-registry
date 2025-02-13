import {httpDelete, httpGetJSON, httpPostEmpty, httpPostJSON, httpPutFile, httpPutJSON, signUpToken} from "./Fetch";
import {appConfig} from "../cfg/config";
import {conditionToQueryString} from "./QueryString";
import {Converter} from "./Converter";
import {ChangeOptions, GetCondition, ListItemApi} from "./ListItemApi";
import {Change, ChangeStatus, Entry, EntryStatus} from "../../domain/Entry";

interface ChangeApiObject {
  changeId: string;
  status: ChangeStatus;
  changeDate: string;
}

interface EntryApiObject {
  did: string;
  status: EntryStatus;
  acceptedChange?: ChangeApiObject;
  proposedChange?: ChangeApiObject;
}

const FAKE: EntryApiObject[] = [
  {did: '123', status: {type: 'some'}, proposedChange: {changeId: '1', status: {type: ''}, changeDate: '123'}, acceptedChange: {changeId: '0', status: {type: 's'}, changeDate: ''}},
  {did: '234', status: {type: 'ret'}, proposedChange: {changeId: '1', status: {type: ''}, changeDate: '123'}, acceptedChange: {changeId: '0', status: {type: 's'}, changeDate: ''}},
  {did: '345', status: {type: 'poi'}, proposedChange: {changeId: '1', status: {type: ''}, changeDate: '123'}, acceptedChange: {changeId: '0', status: {type: 's'}, changeDate: ''}},
  {did: '567', status: {type: 'bebe'}, proposedChange: {changeId: '1', status: {type: ''}, changeDate: '123'}, acceptedChange: {changeId: '0', status: {type: 's'}, changeDate: ''}},
  {did: '678', status: {type: 'ahaha'}, proposedChange: {changeId: '1', status: {type: ''}, changeDate: '123'}, acceptedChange: {changeId: '0', status: {type: 's'}, changeDate: ''}},
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
      // ...obj,
      status: obj.status,
      identity: obj.did
      /*acceptedChange: this.changeConverter.apiObjectToItem(obj.acceptedChange),
      proposedChange: this.changeConverter.apiObjectToItem(obj.proposedChange)*/
    };
    if (obj.acceptedChange) {
      item.acceptedChange =  this.changeConverter.apiObjectToItem(obj.acceptedChange);
    }
    if (obj.proposedChange) {
      item.proposedChange = this.changeConverter.apiObjectToItem(obj.proposedChange);
    }
    return item;
  }

  itemToApiObject(item: Entry): EntryApiObject {
    const res: EntryApiObject = {
      status: item.status, did: item.identity
    };
    if (item.proposedChange) {
      res.proposedChange = this.changeConverter.itemToApiObject(item.proposedChange);
    }
    if (item.acceptedChange) {
      res.acceptedChange = this.changeConverter.itemToApiObject(item.acceptedChange);
    }
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
    console.log(resp);
    return {itemsTotal: resp.itemsTotal || 0, items: resp.items || []};
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
