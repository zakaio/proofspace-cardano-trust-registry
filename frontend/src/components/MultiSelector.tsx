import * as React from 'react';
import Select from 'react-select';

interface IProps {
  allowDuplicates?:boolean;
  values:string[];
  allValues:string[];
  onChange: (values:string[]) => void;
}

export class MultiSelector extends React.Component<IProps> {
  propToCounter = new Map<string, Set<number>>();
  state = {
    values: []
  };

  getPreparedList(list:string[]) {
    if (!this.props.allowDuplicates) {
      return list.map((v) => {
        const obj = {label:v, value: v};
        return obj;
      });
    }
    const result:any[] = [];
    list.forEach((v) => {
      if (!this.propToCounter.has(v)) {
        this.propToCounter.set(v, new Set<number>());
      }
      const set = this.propToCounter.get(v);
      if (set) {
        result.push({label: v, value: `${v}_${set.size}`});
        set.add(set.size);
      }
    });
    return result;
  }

  componentWillMount() {
    this.propToCounter.clear();
    const newValues = this.getPreparedList(this.props.values);
    this.getPreparedList(this.props.allValues);
    this.setState({values: newValues});
  }

  componentWillReceiveProps(nextProps:IProps) {
    this.propToCounter.clear();
    const newValues = this.getPreparedList(nextProps.values);
    this.getPreparedList(nextProps.allValues);
    this.setState({values: newValues});
  }

  changeHandler = (e:any|any[]) => {
    this.propToCounter.clear();
    const newValues:any[] = this.getPreparedList((e || []).map((v:any) => v.label));
    this.getPreparedList(this.props.allValues);
    this.setState(
      {values: newValues},
      () => {
        if (Array.isArray(newValues)) {
          this.props.onChange(newValues.map((item) => item.label));
        }
      }
    );
  };

  render() {
    const options = this.getOptions();
    return (
      <Select
        styles={{
          control: (styles) => ({...styles, minHeight: 16}),
          clearIndicator: (styles) => ({...styles, paddingTop: 2, paddingBottom: 2}),
          dropdownIndicator: (styles) => ({...styles, paddingTop: 2, paddingBottom: 2}),
          input: (styles) => ({...styles}),
          option: (styles) => ({...styles}),
          multiValueLabel: (styles) => ({...styles}),
          multiValue: (styles) => ({...styles}),
        }}
        value={this.state.values}
        isMulti={true}
        menuPlacement={'auto'}
        menuPosition={'fixed'}
        name="colors"
        onChange={this.changeHandler}
        hideSelectedOptions={!!this.props.allowDuplicates}
        options={options}
        className="basic-multi-select"
        classNamePrefix="select"
      />
    );
  }

  getOptions():any[] {
    if (!this.props.allowDuplicates) {
      return this.props.allValues.map((v) => {
        const obj = {label:v, value: v};
        return obj;
      });
    }
    const result:any[] = [];
    this.propToCounter.forEach((v, k) => {
      v.forEach((sValue) => {
        result.push({label: k, value: `${k}_${sValue}`});
      });
    });
    return result;
  }
}
