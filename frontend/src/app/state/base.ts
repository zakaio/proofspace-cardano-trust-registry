export interface ItemsState<T> {
  isLoading?: boolean;
  items: T[];
  itemsTotal: number;
  currentPage: number;
  itemsPerPage: number;
  filter: string;
}
