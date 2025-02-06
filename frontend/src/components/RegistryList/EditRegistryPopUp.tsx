import {FC, useEffect, useState} from "react";
import PopUp from "../PopUp";
import {Registry} from "../../domain/Registry";
import {localize} from "../../app/cfg/Language";
import Panel from "../Panel";
import {calculatePopUpSizes} from "../../utils/domUtil";
import {Button, TextField} from "@mui/material";
import {useDidMount} from "../../app/hooks";
import AlignedHGroup from "../AlignedHGroup";

interface Props {
  open: boolean;
  item?: Registry;
  onCancel: () => void;
  onSave: (item: Registry) => void;
}

const EditRegistryPopUp: FC<Props> = ({item, open, onSave, onCancel}) => {
  const [name, setName] = useState('');
  const [network, setNetwork] = useState('');
  const [subnetwork, setSubnetwork] = useState('');
  const [didPrefix, setDidPrefix] = useState('');

  useEffect(() => {
    setName(item?.name || '');
    setNetwork(item?.network || '');
    setSubnetwork(item?.subnetwork || '');
    setDidPrefix(item?.didPrefix || '');
  }, [item]);

  const submit = () => {
    onSave({identity: item ? item.identity : '', name, network, subnetwork, didPrefix, lastChangeDate: ''})
  };

  const sizes = calculatePopUpSizes({width: 600, height: 800}, {width: 400, height: 450});
  const height = (sizes.height as number) - 192;

  return (
    <PopUp
      title={item ? localize('EDIT_REGISTRY', item.name) : localize('CREATE_REGISTRY')}
      opened={open}
      onClose={onCancel}
    >
      <div style={{paddingLeft: 32, paddingRight: 32}}>
        <Panel style={{height, width: sizes.width || 500}}>
          <AlignedHGroup style={{paddingTop: 32}}>
            <div style={{width: 155, color: '#8E8E8E'}}>{localize('NAME')}</div>
            <div style={{width: 315}}>
              <TextField
                style={{width: 300}}
                error={!name}
                value={name}
                onChange={(evt) => setName(evt.target.value)}
                inputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                InputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                variant="outlined"
              />
            </div>
          </AlignedHGroup>
          <AlignedHGroup style={{paddingTop: 32}}>
            <div style={{width: 155, color: '#8E8E8E'}}>{localize('NETWORK')}</div>
            <div style={{width: 315}}>
              <TextField
                style={{width: 300}}
                error={!network}
                value={network}
                onChange={(evt) => setNetwork(evt.target.value)}
                inputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                InputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                variant="outlined"
              />
            </div>
          </AlignedHGroup>
          <AlignedHGroup style={{paddingTop: 32}}>
            <div style={{width: 155, color: '#8E8E8E'}}>{localize('SUB_NETWORK')}</div>
            <div style={{width: 315}}>
              <TextField
                style={{width: 300}}
                value={subnetwork}
                onChange={(evt) => setSubnetwork(evt.target.value)}
                inputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                InputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                variant="outlined"
              />
            </div>
          </AlignedHGroup>
          <AlignedHGroup style={{paddingTop: 32}}>
            <div style={{width: 155, color: '#8E8E8E'}}>{localize('DID_PREFIX')}</div>
            <div style={{width: 315}}>
              <TextField
                style={{width: 300}}
                error={!didPrefix}
                value={didPrefix}
                onChange={(evt) => setDidPrefix(evt.target.value)}
                inputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                InputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                variant="outlined"
              />
            </div>
          </AlignedHGroup>
        </Panel>
      </div>
      <div style={{textAlign: 'right', padding: 32}}>
        <Button
          variant={'outlined'}
          onClick={onCancel}
          style={{marginRight: 32}}
        >
          {localize('CANCEL')}
        </Button>
        <Button
          variant={'contained'}
          onClick={submit}
        >
          {localize('SUBMIT')}
        </Button>
      </div>
    </PopUp>
  );
};

export default EditRegistryPopUp;
