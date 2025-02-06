import React, {CSSProperties} from "react";
import {FormControl, NativeSelect, Select} from "@mui/material";
import {LabeledItem} from "./LabeledItem";

type DSItem = LabeledItem | "divider";

interface IProps {
  style?: CSSProperties;
  items: DSItem[];
  selected?: any;
  onChange: (value: any) => void;
}

export default class DropSelector extends React.Component<IProps> {
  onChange = (evt: any) => {
    this.props.onChange(evt.target.value);
  };

  render() {
    return (
      <FormControl>
        <NativeSelect value={this.props.selected} onChange={this.onChange}>
          {this.props.items.map((item, index) => {
            if (item === "divider") {
              if (item === "divider") {
                if (index === this.props.items.length - 1) {
                  return "";
                }
                return (
                  <option key={index} disabled>
                    -
                  </option>
                );
                /*return (
                  <Divider key={index}/>
                );*/
              }
            }
            return (
              <option key={index} value={item.value}>
                {item.label}
              </option>
            );
          })}
        </NativeSelect>
      </FormControl>
    );
  }
}

export class PrettyDropSelector extends React.Component<IProps> {
  onChange = (evt: any) => {
    this.props.onChange(evt.target.value);
  };

  render() {
    return (
      <FormControl variant="outlined">
        <Select
          style={this.props.style}
          native
          value={this.props.selected}
          onChange={this.onChange}
          inputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
        >
          {this.props.items.map((item, index) => {
            if (item === "divider") {
              if (index === this.props.items.length - 1) {
                return "";
              }
              return (
                <option key={index} disabled>
                  -
                </option>
              );
              /*return (
                <Divider key={index}/>
              );*/
            }
            return (
              <option key={index} value={item.value}>
                {item.label}
              </option>
            );
          })}
        </Select>
      </FormControl>
    );
  }
}
