import {CSSProperties, default as React} from "react";
import Panel from "./Panel";
import {Tab, Tabs} from "@mui/material";
import {Link} from "react-router-dom";

export interface TabItem {
  label: string;
  value: any;
  link: string;
  disabled?: boolean;
}

interface IProps {
  style?: CSSProperties;
  children?: any;
  disableLinks?: boolean;
  tabItems: TabItem[];
  selectedTabValue: any;
  onTabChange: (newTabValue: any) => void;
}

export default class TabbedPanel extends React.Component<IProps> {
  onChange = (_: any, value: any) => {
    this.props.onTabChange(value);
  };

  render() {
    return (
      <Panel
        style={this.props.style}
        toolbarProps={{
          children: [
            <Tabs
              key={1}
              value={this.props.selectedTabValue}
              TabIndicatorProps={{style: {backgroundColor: '#19B4E1'}}}
              style={{
                borderBottom: "1px solid #C7C7C7",
                minHeight: 32,
                width: "100%",
                paddingLeft: 32
              }}
              onChange={this.onChange}
            >
              {this.props.tabItems.map((item, index) => {
                return this.renderTab(item, index);
              })}
            </Tabs>
          ],
          style: {height: 36}
        }}
      >
        {this.props.children}
      </Panel>
    );
  }

  renderTab(item: TabItem, key: any) {
    if (this.props.disableLinks) {
      return (
        <Tab
          key={key}
          value={item.value}
          label={item.label}
          disabled={item.disabled}
          style={{minWidth: 10, padding: 0, minHeight: 32, marginRight: 32, textTransform: "none"}}
        />
      );
    }

    return (
      <Tab
        key={key}
        component={Link}
        to={item.link}
        value={item.value}
        label={item.label}
        disabled={item.disabled}
        style={{minWidth: 10, padding: 0, minHeight: 32, marginRight: 32, textTransform: "none"}}
      />
    );
  }
}
