import {CSSProperties} from 'react';

export const nodeContains = (element: Node, child: Node) => {
  if (element.contains(child) || (child.parentElement && element.contains(child.parentElement))) {
    return true;
  }
  if (!element.childNodes || !element.childNodes.length) {
    return false;
  }
  for (let i = 0; i < element.childNodes.length; i++) {
    if (nodeContains(element.childNodes[i], child)) {
      return true;
    }
  }

  return false;
};

export interface Sizes {
  width?: number;
  height?: number;
}

export const calculatePopUpSizes = (defaultSize?: Sizes, minSize?: Sizes, maxSizes?: Sizes) => {
  const result: Sizes = {};
  result.width =
    maxSizes && maxSizes.width ? maxSizes.width : document.documentElement.clientWidth - 100;
  result.height =
    maxSizes && maxSizes.height ? maxSizes.height : document.documentElement.clientHeight - 100;
  if (defaultSize && defaultSize.width) {
    result.width = Math.min(result.width, defaultSize.width);
  }
  if (defaultSize && defaultSize.height) {
    result.height = Math.min(result.height, defaultSize.height);
  }
  if (minSize && minSize.width) {
    result.width = Math.max(result.width, minSize.width);
  }
  if (minSize && minSize.height) {
    result.height = Math.max(result.height, minSize.height);
  }
  return result;
};

let isSideBarMinimized = false;

export const changeSideBarState = (isMinimized: boolean) => isSideBarMinimized = isMinimized;

export const getWorkAreaSizes = () => {
  const delta = isSideBarMinimized ? 56 : 200;
  const width = document.documentElement.clientWidth - delta;
  const height = document.documentElement.clientHeight - 65;
  return {width, height};
}
