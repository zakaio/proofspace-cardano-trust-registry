import {CSSProperties, default as React} from "react";
import {removeNullChildren} from "../utils/ReactUtil";

interface IProps {
  style?: CSSProperties;
  children: any;
}

export default class ContentHeader extends React.Component<IProps> {
  render() {
    const children = removeNullChildren(this.props.children);
    const first = children.shift();
    const style = this.props.style || {};
    // const height = style && style.height ? style.height : 92;
    return(
      <div style={{...style, display: 'grid'}}>
        <div style={{display: 'flex', position: 'relative', alignItems: 'center', height: 'auto'}}>
          <div style={{display: 'inline-flex', position: 'relative', flexDirection: 'column', alignItems: 'center'}}>
            {first}
          </div>
          <div style={{flexGrow: 1}} />
          {children.map((child, index) => {
            return (
              <div key={index} style={{display: 'inline-flex', position: 'relative', flexDirection: 'column', alignItems: 'center'}}>
                {child}
              </div>
            );
          })}
        </div>
      </div>
    );
  }
}
