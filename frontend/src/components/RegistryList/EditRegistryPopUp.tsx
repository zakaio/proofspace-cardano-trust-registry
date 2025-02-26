import {FC, useEffect, useState} from "react";
import PopUp from "../PopUp";
import {Contract, Registry} from "../../domain/Registry";
import {localize} from "../../app/cfg/Language";
import Panel from "../Panel";
import {calculatePopUpSizes} from "../../utils/domUtil";
import {Box, Button, Stack, TextField} from "@mui/material";
import {useAppSelector, useDidMount} from "../../app/hooks";
import AlignedHGroup from "../AlignedHGroup";
import {LabeledItem} from "../LabeledItem";
import {cloneDeep, find} from "lodash";
import {PrettyDropSelector} from "../DropSelector";
import {IconButtonWithPopper} from "../Buttons";
import SwitchAccessShortcutAddIcon from '@mui/icons-material/SwitchAccessShortcutAdd';
import {LoaderView} from "../LoaderView";
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import {registryApi} from "../../app/api/API";

/*interface GBProps {
  disabled?: boolean;
  subnetwork: string;
  name: string;
  onCreate: (address: string) => void;
}
const GenerateButton: FC<GBProps> = ({subnetwork, name, onCreate, disabled}) => {
  const templates = useAppSelector((state) => state.registry.cardanoTemplates);
  const [loading, setLoading] = useState(false);
  const [template, setTemplate] = useState('');
  const [params, setParams] = useState<string[]>([]);

  useDidMount(() => {
    if (templates.length) {
      setTemplate(templates[0].name);
    }
  });

  const onSubmit = async () => {
    setLoading(true);
    const address = await registryApi.generateCardanoAddressFromTemplate(subnetwork, name, template, params);
    setLoading(false);
  };

  const onTemplateChange = (tpl: string) => {
    const candidate = find(templates, (t) => t.name === tpl);
    if (candidate) {
      setParams(candidate.parameters?.map((p) => '') || []);
    }
    setTemplate(tpl);
  };

  const createParamHandler = (index: number) => (e: any) => {
    const np = cloneDeep(params);
    np[index] = e.target.value;
    setParams(np);
  };

  const items: LabeledItem[] = templates.map((t) => ({label: t.description, value: t.name}));
  const selected = find(templates, (t) => t.name === template);

  return (
    <IconButtonWithPopper
      disabled={disabled}
      popperStyle={{zIndex: 4000}}
      popperContent={(
        <div>
          {loading ?
            (<LoaderView/>) :
            (<div>
              <PrettyDropSelector items={items} selected={template} onChange={onTemplateChange}/>
              {selected?.parameters?.map((param, index) => {
                return (
                  <div key={index} style={{paddingTop: 16}}>
                    <div>{param.description || param.name}</div>
                    <div>
                      <TextField
                        style={{width: 150}}
                        value={params[index] || ''}
                        onChange={createParamHandler(index)}
                        inputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                        InputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                        variant="outlined"
                      />
                    </div>
                  </div>
                );
              })}
              <div style={{paddingTop: 24}}>
                <Button onClick={onSubmit}>{localize('CREATE')}</Button>
              </div>
            </div>)
          }
        </div>
      )}
    >
      <SwitchAccessShortcutAddIcon/>
    </IconButtonWithPopper>
  );
};*/

interface CGProps {
  disabled?: boolean;
  subnetwork: string;
  name: string;
  onCeate: (contract: Contract) => void;
}
const CardanoGenerator: FC<CGProps> = ({disabled, subnetwork, name, onCeate}) => {
  const templates = useAppSelector((state) => state.registry.cardanoTemplates);
  const [loading, setLoading] = useState(false);
  const [template, setTemplate] = useState('');
  const [params, setParams] = useState<string[]>([]);

  useDidMount(() => {
    if (templates.length) {
      setTemplate(templates[0].name);
    }
  });

  const onSubmit = async () => {
    setLoading(true);
    const contract = await registryApi.generateCardanoAddressFromTemplate(subnetwork, name, template, params);
    setLoading(false);
    onCeate(contract);
  };

  const onTemplateChange = (tpl: string) => {
    const candidate = find(templates, (t) => t.name === tpl);
    if (candidate) {
      setParams(candidate.parameters?.map((p) => '') || []);
    }
    setTemplate(tpl);
  };

  const createParamHandler = (index: number) => (e: any) => {
    const np = cloneDeep(params);
    np[index] = e.target.value;
    setParams(np);
  };

  const items: LabeledItem[] = templates.map((t) => ({label: t.description, value: t.name}));
  const selected = find(templates, (t) => t.name === template);

  if (!name) {
    return (
      <div
        style={{
          borderRadius: 5,
          border: '1px solid rgba(199, 199, 199, 1.0',
          padding: 10,
          width: 465,
          marginTop: 24
        }}
      >
        <AlignedHGroup>
          <div style={{paddingRight: 10}}>
            <Box sx={{display: 'flex'}}>
              <WarningAmberIcon style={{color: '#F39B31'}}/>
            </Box>
          </div>
          <div>{localize('ENTER_NAME_WARNING')}</div>
        </AlignedHGroup>
      </div>
    );
  }
  return (
    <Stack direction={'row'} style={{paddingTop: 32, display: 'flex'}}>
      <div style={{width: 155, color: '#8E8E8E'}}>{localize('GENERATE_CONTRACT')}</div>
      <div>
        {loading ?
          (<LoaderView/>) :
          (<div>
            <PrettyDropSelector items={items} selected={template} onChange={onTemplateChange}/>
            {selected?.parameters?.map((param, index) => {
              return (
                <div key={index} style={{paddingTop: 16}}>
                  <div>{param.description || param.name}</div>
                  <div>
                    <TextField
                      style={{width: 200}}
                      value={params[index] || ''}
                      onChange={createParamHandler(index)}
                      inputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                      InputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                      variant="outlined"
                    />
                  </div>
                </div>
              );
            })}
            <div style={{paddingTop: 24}}>
              <Button onClick={onSubmit}>{localize('CREATE')}</Button>
            </div>
          </div>)
        }
      </div>
    </Stack>
  );
};

interface Props {
  open: boolean;
  item?: Registry;
  onCancel: () => void;
  onSave: (item: Registry) => void;
}

const EditRegistryPopUp: FC<Props> = ({item, open, onSave, onCancel}) => {
  const [name, setName] = useState('');
  const [schema, setSchema] = useState('');
  const [network, setNetwork] = useState('');
  const [subnetwork, setSubnetwork] = useState('');
  const [didPrefix, setDidPrefix] = useState('');
  /*const [proofspaceServiceDid, setProofspaceServiceDid] = useState('');
  const [proofspaceNetwork, setProofspaceNetwork] = useState('');*/
  const [targetAddress, setTargetAddress] = useState('');
  const [addressGenerated, setAddressGenerated] = useState(false);
  const [changeSubmitCost, setChangeSubmitCost] = useState(0);
  const [targetMintingPolicy, setTargetMintingPolicy] = useState('');
  const [submitMintingPolicy, setSubmitMintingPolicy] = useState('');
  const [votingTokenPolicy, setVotingTokenPolicy] = useState('');
  const [votingTokenAsset, setVotingTokenAsset] = useState('');

  const networks = useAppSelector((state) => state.registry.networks);
  console.log('nets', networks);

  useEffect(() => {
    if (open) {
      setAddressGenerated(false)
    }
  }, [open]);
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
    const result: Registry = {identity: item ? item.identity : '', name, schema, network, subnetwork, didPrefix};
    if (network === 'cardano') {
      result.cardano = {
        contract: {
          targetAddress,
          changeSubmitCost,
          targetMintingPolicy,
          submitMintingPolicy,
          votingTokenPolicy,
          votingTokenAsset,
        }
      }
    }
    onSave(result);
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

  const onAddresGenerated = (contract: Contract) => {
    setTargetAddress(contract.targetAddress || '');
    setVotingTokenAsset(contract.votingTokenAsset || '');
    setVotingTokenPolicy(contract.votingTokenPolicy || '');
    setSubmitMintingPolicy(contract.submitMintingPolicy || '');
    setChangeSubmitCost(contract.changeSubmitCost || 0);
    setTargetMintingPolicy(contract.targetMintingPolicy || '');
    setAddressGenerated(true);
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
                disabled={addressGenerated}
                onChange={(evt) => setName(evt.target.value)}
                inputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                InputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                variant="outlined"
              />
            </div>
          </AlignedHGroup>
          <AlignedHGroup style={{paddingTop: 32}}>
            <div style={{width: 155, color: '#8E8E8E'}}>{localize('SCHEMA')}</div>
            <div style={{width: 315}}>
              <TextField
                style={{width: 300}}
                value={schema}
                onChange={(evt) => setSchema(evt.target.value)}
                inputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                InputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                variant="outlined"
              />
            </div>
          </AlignedHGroup>
          <AlignedHGroup style={{paddingTop: 32}}>
            <div style={{width: 155, color: '#8E8E8E'}}>{localize('NETWORK')}</div>
            <div style={{width: 315}}>
              {addressGenerated ?
                (<div>{network}</div>) :
                (<PrettyDropSelector items={networkItems} selected={network} onChange={onNetworkChange}/>)
              }
            </div>
          </AlignedHGroup>
          {!!subNetItems.length && (
            <AlignedHGroup style={{paddingTop: 32}}>
              <div style={{width: 155, color: '#8E8E8E'}}>{localize('SUB_NETWORK')}</div>
              <div style={{width: 315}}>
                {addressGenerated ?
                  (<div>{subnetwork}</div>) :
                  (<PrettyDropSelector items={subNetItems} selected={subnetwork} onChange={(v) => setSubnetwork(v)}/>)
                }
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
          {/*}
          <AlignedHGroup style={{paddingTop: 32}}>
            <div style={{width: 155, color: '#8E8E8E'}}>{localize('PROOFSPACE_SERVICE_DID')}</div>
            <div style={{width: 315}}>
              <TextField
                style={{width: 300}}
                value={proofspaceServiceDid}
                onChange={(evt) => setProofspaceServiceDid(evt.target.value)}
                inputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                InputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                variant="outlined"
              />
            </div>
          </AlignedHGroup>
          <AlignedHGroup style={{paddingTop: 32}}>
            <div style={{width: 155, color: '#8E8E8E'}}>{localize('PROOFSPACE_NETWORK')}</div>
            <div style={{width: 315}}>
              <TextField
                style={{width: 300}}
                value={proofspaceNetwork}
                onChange={(evt) => setProofspaceNetwork(evt.target.value)}
                inputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                InputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                variant="outlined"
              />
            </div>
          </AlignedHGroup>
          {*/}
          {network === 'cardano' && subnetwork.length && !addressGenerated && (<div>
            <CardanoGenerator disabled={!name} subnetwork={subnetwork} name={name} onCeate={onAddresGenerated}/>
          </div>)}
          {network === 'cardano' && addressGenerated && (<div>
          <AlignedHGroup style={{paddingTop: 32}}>
            <div style={{width: 155, color: '#8E8E8E'}}>{localize('TARGET_ADDRESS')}</div>
            <div style={{width: 315}}>
              <TextField
                style={{width: 300}}
                value={targetAddress}
                disabled={addressGenerated}
                // onChange={(evt) => setTargetAddress(evt.target.value)}
                inputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                InputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                variant="outlined"
              />
            </div>
          </AlignedHGroup>
          <AlignedHGroup style={{paddingTop: 32}}>
            <div style={{width: 155, color: '#8E8E8E'}}>{localize('CHANGE_SUBMIT_COST')}</div>
            <div style={{width: 315}}>
              <TextField
                style={{width: 300}}
                type={'number'}
                value={changeSubmitCost}
                // onChange={(evt) => setChangeSubmitCost(parseInt(evt.target.value, 10))}
                inputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                InputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                variant="outlined"
              />
            </div>
          </AlignedHGroup>
          <AlignedHGroup style={{paddingTop: 32}}>
            <div style={{width: 155, color: '#8E8E8E'}}>{localize('TARGET_MINTING_POLICY')}</div>
            <div style={{width: 315}}>
              <TextField
                style={{width: 300}}
                value={targetMintingPolicy}
                // onChange={(evt) => setTargetMintingPolicy(evt.target.value)}
                inputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                InputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                variant="outlined"
              />
            </div>
          </AlignedHGroup>
            <AlignedHGroup style={{paddingTop: 32}}>
              <div style={{width: 155, color: '#8E8E8E'}}>{localize('SUBMIT_MINTING_POLICY')}</div>
              <div style={{width: 315}}>
                <TextField
                  style={{width: 300}}
                  value={submitMintingPolicy}
                  // onChange={(evt) => setSubmitMintingPolicy(evt.target.value)}
                  inputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                  InputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                  variant="outlined"
                />
              </div>
          </AlignedHGroup>
            <AlignedHGroup style={{paddingTop: 32}}>
              <div style={{width: 155, color: '#8E8E8E'}}>{localize('VOTING_TOKEN_POLICY')}</div>
              <div style={{width: 315}}>
                <TextField
                  style={{width: 300}}
                  value={votingTokenPolicy}
                  // onChange={(evt) => setVotingTokenPolicy(evt.target.value)}
                  inputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                  InputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                  variant="outlined"
                />
              </div>
            </AlignedHGroup>
              <AlignedHGroup style={{paddingTop: 32}}>
                <div style={{width: 155, color: '#8E8E8E'}}>{localize('VOTING_TOKEN_ASSET')}</div>
                <div style={{width: 315}}>
                  <TextField
                    style={{width: 300}}
                    value={votingTokenAsset}
                    // onChange={(evt) => setVotingTokenAsset(evt.target.value)}
                    inputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                    InputProps={{style: {paddingTop: 0, paddingBottom: 0, height: 32}}}
                    variant="outlined"
                  />
                </div>
              </AlignedHGroup>
          </div>
          )}
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
          disabled={!name || !didPrefix || (network === 'cardano' && !addressGenerated)}
        >
          {localize('SUBMIT')}
        </Button>
      </div>
    </PopUp>
  );
};

export default EditRegistryPopUp;
