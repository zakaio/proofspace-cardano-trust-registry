import {CSSProperties, default as React} from "react";
import {removeNullChildren} from "../utils/ReactUtil";

interface IProps {
  style?: CSSProperties;
  children: any;
}

export default class AlignedHGroup extends React.Component<IProps> {
  render() {
    const children = removeNullChildren(this.props.children);
    const style = this.props.style || {};
    return(
      <div style={{...style, display: 'grid'}}>
        <div style={{display: 'flex', position: 'relative', alignItems: 'center', height: 'auto'}}>
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
