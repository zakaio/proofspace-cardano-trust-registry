export function rndStr(size: number): string {
  const chars = [..."ABCDEFGHIJKLMNOPQRSTUVWXYZ",
    ..."abcdefghijklmnopwrstuvwxyz",
    ..."0123456789"];
  return [...Array(size)].map(i=>chars[Math.random() * chars.length | 0]).join('');
}
