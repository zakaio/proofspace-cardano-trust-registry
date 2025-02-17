import {wait} from "../../utils/TimeUtils";
import {Converter, StubConverter} from "./Converter";
import {DataRange, Sort} from "./Sort";
import {Attribute} from "./Attribute";
import {Identity} from "../../domain/Identity";

export interface GetCondition<P = undefined> {
  identity?: string | number;
  parent?: P;
  sort?: Sort;
  filters?: Attribute[];
  commonFilter?: string;
  range?: DataRange;
  extended?: any;
}

export interface ChangeOptions<P = undefined> {
  parent?: P;
  prevIdentity?: string | number;
}

export abstract class ListItemApi<T extends Identity, A, P = undefined> {
  protected constructor(public converter: Converter<T, A, P>) {}

  async list(condition?: GetCondition<P>) {
    const resp = await this.fetchList(condition);
    const parent = condition ? condition.parent : undefined;
    return {
      itemsTotal: resp.itemsTotal,
      items: resp.items.map((obj) => this.converter.apiObjectToItem(obj, parent))
    };
  }

  async one(condition?: GetCondition<P>) {
    const obj = await this.fetchOne(condition);
    const parent = condition ? condition.parent : undefined;
    return this.converter.apiObjectToItem(obj, parent);
  }

  async save(item: T, opt?: ChangeOptions<P>) {
    // const id = opt && opt.prevIdentity ? opt.prevIdentity : item.identity;
    await this.fetchSave(this.converter.itemToApiObject(item), opt);
  }

  async create(item: T, opt?: ChangeOptions<P>) {
    const createdObj = await this.fetchCreate(this.converter.itemToApiObject(item), opt);
    const parent = opt ? opt.parent : undefined;
    return this.converter.apiObjectToItem(createdObj, parent);
  }

  async delete(item: T, opt?: ChangeOptions<P>) {
    await this.fetchDelete(this.converter.itemToApiObject(item), opt);
  }

  protected abstract fetchOne(condition?: GetCondition<P>): Promise<A>;
  protected abstract fetchList(
    condition?: GetCondition<P>
  ): Promise<{items: A[]; itemsTotal: number}>;
  protected abstract fetchSave(item: A, opt?: ChangeOptions<P>): Promise<void>;
  protected abstract fetchCreate(item: A, opt?: ChangeOptions<P>): Promise<A>;
  protected abstract fetchDelete(item: A, opt?: ChangeOptions<P>): Promise<void>;
}

export abstract class StubbedListApi<T extends Identity, P = undefined> extends ListItemApi<T, T, P> {
  protected constructor() {
    super(new StubConverter<T>());
  }
  protected async fetchSave(item: T) {
    await wait(200);
  }

  protected async fetchCreate(item: T) {
    await wait(200);
    return item;
  }

  protected async fetchDelete(item: T) {
    await wait(200);
  }
}
