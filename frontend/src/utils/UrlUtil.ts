export const queryToObject = (queryString: string): Record<string, string | undefined> => {
  const pairsString = queryString[0] === '?' ? queryString.slice(1) : queryString;
  const pairs = pairsString.split('&').map((str) => str.split('=').map(decodeURIComponent));
  return pairs.reduce((acc, [key, value]) => (key ? {...acc, [key]: value} : acc), {});
};
