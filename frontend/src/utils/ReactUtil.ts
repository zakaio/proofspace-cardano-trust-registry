import * as React from "react";

export function removeNullChildren(children: any) {
  return React.Children.toArray(children).filter((c) => c);
}
