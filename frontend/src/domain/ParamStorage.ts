class ParamStorage {
  private value: string|undefined;

  store(value: string) {
    this.value = value;
  }

  get() {
    return this.value;
  }

  drop() {
    this.value = undefined;
  }
}

export const paramsStorage = new ParamStorage();
