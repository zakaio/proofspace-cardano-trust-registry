import {FC, useEffect, useState} from "react";
import PopUp from "../PopUp";
import {Registry} from "../../domain/Registry";
import {localize} from "../../app/cfg/Language";
import Panel from "../Panel";
import {calculatePopUpSizes} from "../../utils/domUtil";
import {Button, TextField} from "@mui/material";
import {useAppSelector, useDidMount} from "../../app/hooks";
import AlignedHGroup from "../AlignedHGroup";
import {LabeledItem} from "../LabeledItem";
import {find} from "lodash";
import {PrettyDropSelector} from "../DropSelector";

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

  const networks = useAppSelector((state) => state.registry.networks);
  console.log('nets', networks);

  useEffect(() => {
    setName(item?.name || '');
    const defNet = item?.network ?
      find(networks, (n) => n.network === item.network) :
      networks[0];
    console.log('lets select', item?.network, defNet?.network);
    setNetwork(item?.network || defNet?.network || '');
    let defSubNet = '';
    if (defNet && defNet.subnetworks && defNet.subnetworks.length) {
      defSubNet = defNet.subnetworks[0];
    }
    setSubnetwork(item?.subnetwork || defSubNet);
    setDidPrefix(item?.didPrefix || '');
  }, [item, networks]);

  const submit = () => {
    onSave({identity: item ? item.identity : '', name, network, subnetwork, didPrefix, lastChangeDate: ''})
  };

  const onNetworkChange = (v: string) => {
    const net = find(networks, (n) => n.network === v);
    if (net) {
      let subNet = '';
      if (net && net.subnetworks && net.subnetworks.length) {
        subNet = net.subnetworks[0];
      }
      setNetwork(v);
      setSubnetwork(subNet);
    }
  };

  const sizes = calculatePopUpSizes({width: 600, height: 800}, {width: 400, height: 450});
  const height = (sizes.height as number) - 192;

  const networkItems: LabeledItem[] =
    networks.map((n) => ({label: n.network, value: n.network}));
  const selectedNetwork = find(networks, (n) => n.network === network);
  console.log('selected network', selectedNetwork);
  let subNetItems: LabeledItem[] = [];
  if (selectedNetwork && selectedNetwork.subnetworks) {
    console.log('yes. we have subnets', selectedNetwork.subnetworks);
    subNetItems = selectedNetwork.subnetworks.map((s) => ({label: s, value: s}));
  }
  console.log('subNetItems', subNetItems);

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
              <PrettyDropSelector items={networkItems} selected={network} onChange={onNetworkChange}/>
            </div>
          </AlignedHGroup>
          {!!subNetItems.length && (
            <AlignedHGroup style={{paddingTop: 32}}>
              <div style={{width: 155, color: '#8E8E8E'}}>{localize('SUB_NETWORK')}</div>
              <div style={{width: 315}}>
                <PrettyDropSelector items={subNetItems} selected={subnetwork} onChange={(v) => setSubnetwork(v)}/>
              </div>
            </AlignedHGroup>
          )}
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
