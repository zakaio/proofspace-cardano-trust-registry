import {CSSProperties, FC, Component} from "react";
import InfoIcon from '@mui/icons-material/Info';
import {localize} from '../app/cfg/Language';
import {PopperPlacementType} from '@mui/material/Popper';
import {ClickAwayListener, Fade, IconButton, Paper, Popper} from "@mui/material";

interface IIBWPProps {
  style?: CSSProperties;
  popperStyle?: CSSProperties;
  disabled?: boolean;
  children: any;
  popperContent: any;
  placement?: PopperPlacementType;
  popperMaxHeight?: number;
  popperWidth?: number;
}

class IconButtonWithPopper extends Component<IIBWPProps> {
  state = {opened: false, anchorEl: undefined};

  onButtonClick = (evt: any) => {
    const opened = !this.state.opened;
    this.setState({opened, anchorEl: evt.currentTarget});
  };

  onClickAway = () => this.setState({opened: false});

  render() {
    const style: CSSProperties = this.props.style || {};
    const s = this.props.style || {};
    const popperWidth = this.props.popperWidth || s.width;
    const ws = popperWidth ? {width: popperWidth} : {};
    return (
      <ClickAwayListener onClickAway={this.onClickAway}>
        <div>
          <Popper
            style={this.props.popperStyle}
            open={this.state.opened}
            anchorEl={this.state.anchorEl}
            placement={this.props.placement || "right-start"}
            transition
          >
            {({TransitionProps}) => (
              <Fade {...TransitionProps} timeout={200}>
                <Paper elevation={3}>
                  <div
                    style={{
                      ...ws,
                      maxHeight: this.props.popperMaxHeight || 400,
                      overflow: "auto"
                    }}
                  >
                    {this.props.popperContent}
                  </div>
                </Paper>
              </Fade>
            )}
          </Popper>
          <IconButton style={style} disabled={this.props.disabled} onClick={this.onButtonClick}>
            {this.props.children}
          </IconButton>
        </div>
      </ClickAwayListener>
    );
  }
}

export function localizedInnerHtml(key: string, ...params: any[]) {
  return {__html: localize(key, ...params)};
}

interface IProps {
  placement?: PopperPlacementType;
  children: any;
}

export const HintIcon: FC<IProps> = ({placement, children}) => {
    return (
      <IconButtonWithPopper
        placement={placement}
        popperStyle={{zIndex: 5000}}
        popperContent={<div style={{padding: 16}}>{children}</div>}
        popperWidth={300}
      >
        <InfoIcon />
      </IconButtonWithPopper>
    );
}
