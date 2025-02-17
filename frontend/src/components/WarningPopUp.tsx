import * as React from "react";
import WarningIcon from '@mui/icons-material/Warning';
import {localize} from "../app/cfg/Language";
import PopUp from "./PopUp";
import Panel from "./Panel";
import AlignedHGroup from "./AlignedHGroup";
import {Button} from "@mui/material";

interface IProps {
  opened:boolean;
  title: string;
  message: string;
  onConfirm: () => void;
  onCancel: () => void;
}

export class WarningPopUp extends React.Component<IProps> {
  render() {
    return (
      <PopUp
        opened={this.props.opened}
        title={this.props.title}
        onClose={this.props.onCancel}
      >
        <div style={{paddingLeft: 32, paddingRight: 32}}>
          <Panel style={{height: 150}}>
            <AlignedHGroup>
              <div style={{paddingRight: 32}}>
                <WarningIcon/>
              </div>
              <div>
                {this.props.message}
              </div>
            </AlignedHGroup>
          </Panel>
        </div>
        <div style={{textAlign: 'right', padding: 32}}>
          <Button variant={'outlined'} onClick={this.props.onCancel} style={{marginRight: 16}}>{localize('CANCEL')}</Button>
          <Button variant={'contained'} onClick={this.props.onConfirm}>{localize('SUBMIT')}</Button>
        </div>
      </PopUp>
    );
  }
}
