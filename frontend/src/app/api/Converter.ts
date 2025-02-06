import {Identity} from "../../domain/Identity";

export abstract class Converter<T extends Identity, A, P = undefined> {
  abstract apiObjectToItem(obj: A, parent?: P): T;
  abstract itemToApiObject(item: T): A;
}

export class StubConverter<T extends Identity> extends Converter<T, T, undefined> {
  apiObjectToItem(obj: T, parent?: undefined): T {
    return obj;
  }

  itemToApiObject(item: T): T {
    return item;
  }
}
