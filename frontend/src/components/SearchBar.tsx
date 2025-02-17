import {CSSProperties, default as React} from "react";
import {IconButton, InputAdornment, TextField} from "@mui/material";
import ClearIcon from "@mui/icons-material/Clear";

const ENTER = 13;

export interface SearchFilter {
  id: string;
  selected?: any;
  style?: CSSProperties;
}

interface IProps {
  style?: CSSProperties;
  showBorder?: boolean;
  textFilter?: string;
  filters?: SearchFilter[];
  rightElement?: any;
  onSearchFilterChange?: (id: string, value: any) => void;
  onTextFilterChange: (filter: string) => void;
  placeholder?: string;
}

export default class SearchBar extends React.Component<IProps> {
  state = {
    textFilter: ""
  };

  componentWillMount() {
    this.setState({textFilter: this.props.textFilter || ""});
  }

  componentWillReceiveProps(nextProps: Readonly<IProps>, nextContext: any): void {
    if (
      nextProps.textFilter !== this.props.textFilter &&
      nextProps.textFilter !== this.state.textFilter
    ) {
      this.setState({textFilter: nextProps.textFilter || ""});
    }
  }

  createFilterHandler = (id: string) => (value: any) => {
    if (this.props.onSearchFilterChange !== undefined) {
      this.props.onSearchFilterChange(id, value);
    }
  };

  render() {
    const filters = this.props.filters || [];
    return (
      <div style={this.props.style}>
        <div style={{display: "flex", position: "relative", alignItems: "center", height: "auto"}}>
          <div
            style={{
              display: "inline-flex",
              position: "relative",
              flexDirection: "column",
              alignItems: "center"
            }}
          >
            {this.renderSearchField()}
          </div>
          <div style={{flexGrow: 1}} />
          <div
            style={{
              display: "inline-flex",
              position: "relative",
              flexDirection: "column",
              alignItems: "center"
            }}
          >
            {this.props.rightElement}
          </div>
        </div>
      </div>
    );
  }

  onFilterKeyUp = (evt: any) => {
    if (evt.keyCode === ENTER) {
      this.props.onTextFilterChange(this.state.textFilter);
    } // else if (evt.keyCode === ESC) {}
  };

  onTextFilterChange = (evt: any) => {
    this.setState({textFilter: evt.target.value});
  };

  onDropTextFilter = () => {
    this.setState({textFilter: ""}, () => this.props.onTextFilterChange(""));
  };

  renderSearchField() {
    return (
      <TextField
        placeholder={this.props.placeholder || "Search"}
        style={{width: 270, height: 32, maxHeight: 32, marginTop: 0, marginBottom: 0}}
        inputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
        InputProps={{
          style: {paddingTop: 0, paddingBottom: 0, height: 32, paddingRight: 8},
          endAdornment: (
            <InputAdornment position="end">
              <IconButton
                onClick={this.onDropTextFilter}
                disabled={!this.state.textFilter}
                style={{padding: 0, borderRadius: "none"}}
                size="large">
                <ClearIcon />
              </IconButton>
            </InputAdornment>
          )
        }}
        value={this.state.textFilter}
        onKeyUp={this.onFilterKeyUp}
        onChange={this.onTextFilterChange}
        margin={"normal"}
        variant="outlined"
      />
    );
  }
}
