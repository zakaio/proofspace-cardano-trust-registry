import {httpDelete, httpGetJSON, httpPostEmpty, httpPostJSON, httpPutFile, httpPutJSON, signUpToken} from "./Fetch";
import {appConfig} from "../cfg/config";
import {conditionToQueryString} from "./QueryString";
import {Converter} from "./Converter";
import {ChangeOptions, GetCondition, ListItemApi} from "./ListItemApi";
import {ProposedChange} from "../../domain/ProposedChange";

interface ProposedChangeApiObject {
  changeId: string;
  registryId: string;
  addedDids: string[];
  removedDids: string[];
  approved: boolean;
  changeDate: string;
  transactionId: string;
}

class ProposedChangeConverter extends Converter<ProposedChange, ProposedChangeApiObject> {
  apiObjectToItem(obj: ProposedChangeApiObject): ProposedChange {
    return {...obj, identity: obj.changeId};
  }

  itemToApiObject(item: ProposedChange): ProposedChangeApiObject {
    return {...item, changeId: item.identity};
  }
}


const converterObject = {
  identity: 'changeId',
};

export class ProposedChangesApi extends ListItemApi<ProposedChange, ProposedChangeApiObject, string> {
  constructor() {
    super(new ProposedChangeConverter());
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

  protected async fetchOne(condition?: GetCondition<string>): Promise<ProposedChangeApiObject> {
    const id = condition && condition.identity ? condition.identity : '';
    return await httpGetJSON(`${appConfig().BACKEND}/trust-registry/${encodeURI(id as string)}`);
  }

  protected async fetchList(condition?: GetCondition<string>) {
    // return {itemsTotal: 32, items: FAKE};
    const resp = await httpGetJSON(
      `${appConfig().BACKEND}/trust-registry/${condition?.parent || ''}/changes${conditionToQueryString(condition, converterObject)}`
    );
    console.log(resp);
    return {itemsTotal: resp.itemsTotal || 0, items: resp.items || []};
  }

  protected async fetchCreate(item: ProposedChangeApiObject): Promise<ProposedChangeApiObject> {
    return item;
  }

  protected async fetchSave(item: ProposedChangeApiObject, opt?: ChangeOptions<string>): Promise<void> {
    console.log('i call save');
    return;
  }

  protected async fetchDelete(item: ProposedChangeApiObject): Promise<void> {
    // await httpDelete(`${appConfig().BACKEND}/project/${encodeURI(item.did)}`);
  }

}
