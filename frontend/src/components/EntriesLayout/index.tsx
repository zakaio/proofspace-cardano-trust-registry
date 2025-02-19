import {FC, useState} from "react";
import {Box, Breadcrumbs, Button, IconButton, Table} from "@mui/material";
import AddIcon from '@mui/icons-material/Add';
import ContentHeader from "../ContentHeader";
import AlignedHGroup from "../AlignedHGroup";
import {localize} from "../../app/cfg/Language";
import {HintIcon, localizedInnerHtml} from "../HintIcon";
import {useAppDispatch, useAppSelector, useDidMount} from "../../app/hooks";
import {Link, useParams} from "react-router-dom";
import {createLink} from "../../app/cfg/config";
import {MainPath, RegistryPath} from "../../app/cfg/RoutePath";
import ThumbUpIcon from '@mui/icons-material/ThumbUp';
import ThumbDownIcon from '@mui/icons-material/ThumbDown';
import dateFormat from "dateformat";
import {getWorkAreaSizes} from "../../utils/domUtil";
import ChangesPopUp from "./EntriesList/ChangesPopUp";
import {proposeChanges} from "../../app/state/proposedChanges";
import TabbedPanel, {TabItem} from "../TabbedPanel";
import ChangesList from "./ChangesList";
import EntriesList from "./EntriesList";
import {find} from "lodash";

const tabItems = (registryId: string) => {
  const result: TabItem[] = [
    {
      label: localize('ENTRIES'),
      value: RegistryPath.ENTRIES,
      link: createLink(`${MainPath.REGISTRY}/${registryId}/${RegistryPath.ENTRIES}`)
    },
    {
      label: localize('CHANGES'),
      value: RegistryPath.CHANGES,
      link: createLink(`${MainPath.REGISTRY}/${registryId}/${RegistryPath.CHANGES}`)
    },
  ];
  return result;
};

const EntriesLayout: FC<{}> = () => {
  const {id, section} = useParams();
  const dispatch = useAppDispatch();

  const [open, setOpen] = useState(false);
  const registries = useAppSelector((state) => state.registry.items);
  const selected = find(registries, (r) => r.identity === id);

  const onProposeChanges = (added: string[], removed: string[]) => {
    setOpen(false);
    dispatch(proposeChanges({registryId: id || '', added, removed}));
  }

  const sizes = getWorkAreaSizes();

  return (
    <Box width={'100%'}>
      <ChangesPopUp open={open} added={[]} removed={[]} onCancel={() => setOpen(false)} onSave={onProposeChanges}/>
      <ContentHeader>
        <AlignedHGroup>
          <Breadcrumbs>
            <Link
              to={createLink(MainPath.ALL)}
              style={{color: 'inherit', textDecoration: 'none'}}
            >
              <div style={{fontSize: 18, fontWeight: 600}}>{localize('REGISTRIES_LIST')}</div>
            </Link>
            <div style={{fontSize: 18, fontWeight: 600, paddingRight: 32}}>
              {id || ''} {selected && selected.schema ? `(${localize('SCHEMA')}: ${selected.schema})` : ''}
            </div>
          </Breadcrumbs>
          <div>
            <HintIcon>
              <div dangerouslySetInnerHTML={localizedInnerHtml('HINTS_ENTRIES_LIST')}/>
            </HintIcon>
          </div>
        </AlignedHGroup>
        <AlignedHGroup>
          <div>
            <Button variant={'outlined'} startIcon={<AddIcon/>} onClick={() => setOpen(true)}>
              {localize('PROPOSE_CHANGE')}
            </Button>
          </div>
        </AlignedHGroup>
      </ContentHeader>
      <div style={{paddingTop: 16}}/>
      <Box sx={{width: 1, borderTop: '1px solid rgb(199, 199, 199)'}}>
        <TabbedPanel
          tabItems={tabItems(id || '')}
          selectedTabValue={section || RegistryPath.ENTRIES}
          onTabChange={(v) => console.log(v)}
          style={{height: sizes.height - 131}}
        >
          <div style={{paddingTop: 16}}/>
          {section === RegistryPath.CHANGES ?
            (<ChangesList registryId={id || ''}/>) :
            (<EntriesList registryId={id || ''}/>)
          }
        </TabbedPanel>
      </Box>
    </Box>
  );
};

export default EntriesLayout;
