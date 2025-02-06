import {CSSProperties, FC, Component} from "react";
import {Button, ClickAwayListener, Fade, IconButton, Paper, Popper, PopperPlacementType} from "@mui/material";

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

export class IconButtonWithPopper extends Component<IIBWPProps> {
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
