export const wait = async (delay: number) => new Promise<void>((resolve) => {
  setTimeout(resolve, delay);
});

export const timeZoneOffsetInHours = (d: Date) => {
  const offset = -1 * d.getTimezoneOffset() / 60;
  return offset > 0 ? `+${offset}` : `${offset}`;
};
