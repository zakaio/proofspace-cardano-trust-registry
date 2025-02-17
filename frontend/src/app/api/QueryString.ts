import {GetCondition} from "./ListItemApi";

export function conditionToQueryString (condition?: GetCondition<any>, convertObject?: any) {
  if (!condition) {
    return '';
  }
  const parts = [];
  if (condition.range) {
    const range = condition.range;
    if (range.limit) {
      parts.push(`limit=${range.limit}`);
    }
    if (range.offset) {
      parts.push(`offset=${range.offset}`);
    }
  }
  if (condition.sort) {
    const sort = condition.sort;
    if (sort.by) {
      parts.push(`orderBy=${convert(sort.by, convertObject)}`);
    }
    if (sort.order) {
      parts.push(`orderDirection=${sort.order}`);
    }
  }
  if (condition.commonFilter) {
    parts.push(`filter=${condition.commonFilter}`)
  }
  if (condition.filters) {
    condition.filters.forEach((f) => parts.push(`${convert(f.name, convertObject)}=${f.value}`));
  }

  if (!parts.length) {
    return '';
  }

  return `?${parts.join('&')}`;
}

const convert = (field: string, convertObject?: any) => {
  if (!convertObject) {
    return field;
  }
  return convertObject[field] || field;
};
