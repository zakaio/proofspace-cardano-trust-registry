import {httpGetJSON, httpPostJSON, token} from './Fetch';
import {appConfig} from '../cfg/config';
import {wait} from '../../utils/TimeUtils';
import {User} from "../../domain/User";
import {rndStr} from "../../utils/Random";


export class AuthApi {

  async getStatus(): Promise<User|undefined> {
    /*await wait(3000);
    return {
      email: 'test@test.test',
      serviceRecords:
    };*/
    if (!token()) {
      return undefined;
    }

    let resp: User | undefined = undefined;
    try {
      resp = await httpGetJSON('', token(), {ignoreErrors: true});
      console.log('we have user', resp);
      // if (resp) resp.serviceRecords = FAKE_SERVICE_RECORDS;
    } catch (e) {
      console.log('error', e);
      // do nothing
    }
    return resp;
  }

}
