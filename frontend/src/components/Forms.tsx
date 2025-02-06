import {CSSProperties, Component} from "react";
import {Checkbox, FormControlLabel, Radio, RadioGroup} from "@mui/material";
import CheckBoxOutlineBlankIcon from "@mui/icons-material/CheckBoxOutlineBlank";
import CheckBoxIcon from "@mui/icons-material/CheckBox";

interface ICheckProps {
  name?: string;
  checked?: boolean;
  disabled?: boolean;
  stopClickEvent?: boolean;
  style?: CSSProperties;
  onChange?: () => void;
  label?: string;
}

export class CheckBoxInGroup extends Component<ICheckProps> {
  render() {
    const {style, disabled, checked, onChange, name, label, stopClickEvent} = this.props || {};
    const preventedHandler = (evt: any) => {
      evt.preventDefault();
      evt.stopPropagation();
      if (onChange !== undefined) {
        onChange();
      }
    }
    return (
      <FormControlLabel
        style={{display: "block", margin: 0}}
        disabled={disabled}
        control={
          <Checkbox
            checked={checked}
            style={{/*color: Styles.Forms.Item.COLOR,*/ ...style}}
            onClick={preventedHandler}
            name={name}
            disabled={disabled}
          />
        }
        label={label || ""}
      />
    );
  }
}

export class SimpleCheckbox extends Component<ICheckProps> {
  preventedHandler = (evt: any) => {
    evt.preventDefault();
    evt.stopPropagation();
    const onChange = this.props.onChange;
    if (onChange !== undefined) {
      onChange();
    }
  }
  render() {
    return (
      <Checkbox
        checked={this.props.checked}
        disabled={this.props.disabled}
        onClick={this.preventedHandler}
        // onChange={this.props.onChange}
        style={{padding: 0, width: 32, height: 32, opacity: this.props.disabled ? 0.5 : 1}}
        icon={<CheckBoxOutlineBlankIcon style={{fontSize: 24/*, color: Styles.Forms.Item.COLOR*/}} />}
        checkedIcon={<CheckBoxIcon style={{fontSize: 24/*, color: Styles.Forms.Item.COLOR*/}} />}
      />
    );
  }
}

export class SmallCheckbox extends Component<ICheckProps> {
  render() {
    return (
      <Checkbox
        checked={this.props.checked}
        disabled={this.props.disabled}
        onChange={this.props.onChange}
        style={{padding: 0, width: 32, height: 32, opacity: this.props.disabled ? 0.5 : 1}}
        icon={<CheckBoxOutlineBlankIcon style={{fontSize: 20/*, color: Styles.Forms.Item.COLOR*/}} />}
        checkedIcon={<CheckBoxIcon style={{fontSize: 20/*, color: Styles.Forms.Item.COLOR*/}} />}
      />
    );
  }
}

interface IRadioProps {
  style?: CSSProperties;
  value: any;
  label?: string;
  disabled?: boolean;
}

export class RadioInGroup extends Component<IRadioProps> {
  render() {
    const {style, value, disabled, label} = this.props || {};
    return (
      <FormControlLabel
        style={{display: "block"}}
        value={value}
        disabled={disabled}
        control={<Radio style={{/*color: Styles.Forms.Item.COLOR,*/ ...style}} />}
        label={label}
      />
    );
  }
}

interface IStandaloneRadioProps<T = string> {
  name?: string;
  value?: T;
  checked?: boolean;
  disabled?: boolean;
  style?: CSSProperties;
  onChange?: (v?: T) => void;
  label?: string;
}

export class StandaloneRadioButton extends Component<IStandaloneRadioProps> {
  onChange = () => this.props.onChange?.(this.props.value);

  render() {
    const {name, disabled, checked, value} = this.props || {};

    return (
      <Radio
        checked={checked}
        disabled={disabled}
        onChange={this.onChange || undefined}
        name={name}
        value={value}
        style={{
          padding: 0,
          width: 32,
          height: 32,
          //color: Styles.Forms.Item.COLOR,
          opacity: disabled ? 0.5 : 1
        }}
      />
    );
  }
}

interface RBGroupProps<T = string> {
  style?: CSSProperties;
  items: any;
  value: T;
  name: string;
  onChange: (value: T) => void;
}

export class RBGroup<T = string> extends Component<RBGroupProps<T>> {
  onChange = (evt: any) => this.props.onChange(evt.target.value);

  render() {
    const {style: s, items, name, value} = this.props || {};
    return (
      <RadioGroup name={name} value={value} onChange={this.onChange}>
        {items.map((item: any, i: number) => {
          return (
            <FormControlLabel
              key={i}
              style={{display: "block"}}
              value={item.value}
              disabled={item.disabled}
              control={<Radio style={{/*color: Styles.Forms.Item.COLOR,*/ ...s}} />}
              label={item.label}
            />
          );
        })}
      </RadioGroup>
    );
  }
}
