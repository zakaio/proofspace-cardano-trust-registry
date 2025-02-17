import {CSSProperties, default as React} from "react";
import CloseIcon from '@mui/icons-material/Close';
import ContentHeader from "./ContentHeader";
import {Dialog, IconButton, Typography} from "@mui/material";

interface IProps {
  style?: CSSProperties;
  title: string;
  opened: boolean;
  onClose: () => void;
  children?: any;
  minWidth?: number;
  headerChildren?: any;
  disableEscapeKeyDown?: boolean;
  disableEnforceFocus?: boolean;
  disableRestoreFocus?: boolean;
  disableScrollLock?: boolean;
}

export default class PopUp extends React.Component<IProps> {
  render() {
    return (
      <Dialog
        maxWidth={false}
        PaperProps={{style: {borderRadius: 5}}}
        onClose={this.props.onClose}
        open={this.props.opened}
        disableEnforceFocus={this.props.disableEnforceFocus}
        disableEscapeKeyDown={this.props.disableEscapeKeyDown}
        disableRestoreFocus={this.props.disableRestoreFocus}
        disableScrollLock={this.props.disableScrollLock}
      >
        <div style={{paddingLeft: 32, paddingRight: 32}}>
          <ContentHeader style={{minWidth: this.props.minWidth || 700}}>
            <Typography variant="h6">{this.props.title}</Typography>
            {this.props.headerChildren}
            <IconButton onClick={this.props.onClose}>
              <CloseIcon />
            </IconButton>
          </ContentHeader>
        </div>
        <div>
          {this.props.children}
        </div>
      </Dialog>
    );
  }
}
