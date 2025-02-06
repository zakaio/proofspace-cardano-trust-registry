import {FC, useEffect, useState} from "react";
import PopUp from "../PopUp";
import {localize} from "../../app/cfg/Language";
import Panel from "../Panel";
import {calculatePopUpSizes} from "../../utils/domUtil";
import {Button, TextField} from "@mui/material";
import {useDidMount} from "../../app/hooks";
import AlignedHGroup from "../AlignedHGroup";

interface Props {
  open: boolean;
  added: string[];
  removed: string[];
  onCancel: () => void;
  onSave: (added: string[], removed: string[]) => void;
}

const ChangesPopUp: FC<Props> = ({added, removed, open, onSave, onCancel}) => {
  const [addedList, setAddedList] = useState('');
  const [removedList, setRemovedList] = useState('');

  useEffect(() => {
    setAddedList(added.join('\n'));
    setRemovedList(removed.join('\n'));
  }, [added, removed]);

  const submit = () => {
    onSave(addedList.split('\n'), removedList.split('\n'));
  };

  const sizes = calculatePopUpSizes({width: 600, height: 800}, {width: 400, height: 450});
  const height = (sizes.height as number) - 192;

  return (
    <PopUp
      title={localize('PROPOSE_CHANGES')}
      opened={open}
      onClose={onCancel}
    >
      <div style={{paddingLeft: 32, paddingRight: 32}}>
        <Panel style={{height, width: sizes.width || 500}}>
          <AlignedHGroup style={{paddingTop: 32}}>
            <div style={{width: 155, color: '#8E8E8E'}}>{localize('ADDED_DIDS')}</div>
            <div style={{width: 315}}>
              <TextField
                multiline={true}
                rows={5}
                style={{width: 300}}
                value={addedList}
                onChange={(evt) => setAddedList(evt.target.value)}
                inputProps={{style: {paddingTop: 4, paddingBottom: 4}}}
                InputProps={{style: {paddingTop: 4, paddingBottom: 4}}}
                variant="outlined"
              />
            </div>
          </AlignedHGroup>
          <AlignedHGroup style={{paddingTop: 32}}>
            <div style={{width: 155, color: '#8E8E8E'}}>{localize('REMOVED_DIDS')}</div>
            <div style={{width: 315}}>
              <TextField
                multiline={true}
                rows={5}
                style={{width: 300}}
                value={removedList}
                onChange={(evt) => setRemovedList(evt.target.value)}
                inputProps={{style: {paddingTop: 4, paddingBottom: 4}}}
                InputProps={{style: {paddingTop: 4, paddingBottom: 4}}}
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

export default ChangesPopUp;
