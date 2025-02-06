export enum SortOrder {
  DESC = "desc",
  ASC = "asc"
}

export interface Sort {
  by: string;
  order: SortOrder;
}

export interface DataRange {
  limit?: number;
  offset?: number;
}

export const invertOrder = (order?: SortOrder) => {
  if (order === SortOrder.DESC) {
    return SortOrder.ASC;
  }
  return SortOrder.DESC;
};
