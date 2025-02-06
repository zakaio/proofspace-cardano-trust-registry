import * as React from 'react';
import PreviewIcon from '@mui/icons-material/InsertPhoto';
import {FC} from "react";

interface Props {
  icon?: string;
  minSize?: number;
  maxSize?: number;
}

const IconPreview: FC<Props> = (props) => {
  const minSize = props.minSize || 100;
  const maxSize = props.maxSize || 200;
  const renderIcon = () => {
    const icon = props.icon;
    if (!icon) {
      return (
        <PreviewIcon style={{width: minSize, height: minSize, color: 'rgb(97, 97, 104)'}}/>
      );
    }
    return (
      <img style={{maxWidth: maxSize, maxHeight: maxSize, minWidth: minSize, minHeight: minSize}} src={icon} alt={''}/>
    );
  }

  return (
    <div style={{minWidth: minSize, minHeight: minSize, maxWidth: maxSize, maxHeight: maxSize, backgroundColor: '#e3e3e3'}}>
      {renderIcon()}
    </div>
  );
};

export default  IconPreview;
