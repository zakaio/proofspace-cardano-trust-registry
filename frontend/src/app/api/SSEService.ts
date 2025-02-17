import {sseUri} from "../cfg/config";

class SSEService {
  private source!: EventSource;
  private errorHandler!: (msg: string) => void;
  private tokenHandler!: (token: string) => void;

  init(deviceCode: string, tokenHandler: (token: string) => void, errorHandler: (msg: string) => void) {
    this.clear();
    this.tokenHandler = tokenHandler;
    this.errorHandler = errorHandler;
    try {
      this.source = new EventSource(sseUri(deviceCode));
      this.source.addEventListener('token', this.onToken);
      this.source.addEventListener('error', this.onError);
    } catch (e) {
      console.log('SSE error!', e);
    }
  }

  private clear() {
    if (this.source) {
      this.source.removeEventListener('token', this.onToken);
      this.source.removeEventListener('error', this.onError);
      this.source.close();
    }
  }

  private onToken = (evt: any) => {
    this.clear();
    this.tokenHandler(evt.data);
  };

  private onError = (evt: any) => {
    this.clear();
    console.log('Error', evt, evt.message);
    this.errorHandler(evt.message);
  };
}

export const sseService = new SSEService();
