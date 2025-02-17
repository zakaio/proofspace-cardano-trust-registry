import * as React from 'react';
import {CSSProperties} from "react";

const defaultToolbarStyle: CSSProperties = {
  height: 32,
  minHeight: 32,
};

export interface ToolbarProps {
  style?: CSSProperties;
  children?: any;
}

interface IProps {
  panelId?: string;
  toolbarProps?: ToolbarProps;
  style?: CSSProperties;
  children?: any;
}

export default class Panel extends React.Component<IProps> {
  toolbarHref = React.createRef<HTMLDivElement>();
  toolsHeight = 0;

  render(): React.ReactNode {
    this.toolsHeight = 0;
    return (
      <div id={this.props.panelId} style={{overflow: 'hidden', height: '100%', width: '100%'}}>
        {this.renderToolbar()}
        {this.renderContent()}
      </div>
    );
  }

  renderToolbar() {
    if (!this.props.toolbarProps) {
      return '';
    }
    const props: any = this.props.toolbarProps || {};
    const style = props.style;
    if (style) {
      this.toolsHeight += style.height as number;
    }
    return (
      <div ref={this.toolbarHref} style={{...style, overflow: 'hidden'}}>
        {props.children}
      </div>
    );
  }

  renderContent() {
    const style: CSSProperties = this.props.style || {};
    let height: string|number = '100%';
    if (style && style.height) {
      if (typeof style.height === 'number') {
        height = style.height as number - this.toolsHeight;
      } else {
        height = `calc(${style.height}-${this.toolsHeight})`;
      }
    }
    return (
      <div style={{...style,
        overflow: 'auto',
        width: '100%',
        height
      }}>
        {this.props.children}
      </div>
    );
  }
}
