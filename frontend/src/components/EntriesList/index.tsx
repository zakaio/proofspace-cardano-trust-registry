import {FC, useState} from "react";
import {Box, Breadcrumbs, Button, Table} from "@mui/material";
import AddIcon from '@mui/icons-material/Add';
import ContentHeader from "../ContentHeader";
import AlignedHGroup from "../AlignedHGroup";
import {localize} from "../../app/cfg/Language";
import {HintIcon, localizedInnerHtml} from "../HintIcon";
import {useAppDispatch, useAppSelector, useDidMount} from "../../app/hooks";
import SearchBar from "../SearchBar";
import Paginator from "../Paginator";
import {LoaderView} from "../LoaderView";
import TableList, {Column} from "../TableList";
import {Entry} from "../../domain/Entry";
import {getEntries, proposeChanges} from "../../app/state/entries";
import {Link, useParams} from "react-router-dom";
import {createLink} from "../../app/cfg/config";
import {MainPath} from "../../app/cfg/RoutePath";
import ChangesPopUp from "./ChangesPopUp";

const EntriesList: FC<{}> = () => {
  const [open, setOpen] = useState(false);
  const {id} = useParams();
  const dispatch = useAppDispatch();
  const items = useAppSelector((state) => state.entry.items);
  const itemsTotal = useAppSelector((state) => state.entry.itemsTotal);
  const currentPage = useAppSelector((state) => state.entry.currentPage);
  const itemsPerPage = useAppSelector((state) => state.entry.itemsPerPage);
  const filter = useAppSelector((state) => state.entry.filter);
  const isLoading = useAppSelector((state) => state.entry.isLoading);

  useDidMount(() => dispatch(getEntries({registryId: id || '', filter, itemsPerPage, currentPage})));

  const onFilterChange = (f: string) => dispatch(getEntries({registryId: id || '', currentPage, itemsPerPage, filter: f}));
  const setPage = (page: number) => dispatch(getEntries({registryId: id || '', currentPage: page, itemsPerPage, filter}));
  const setItemsPerPage = (perPage: number) => dispatch(getEntries({registryId: id || '', currentPage, itemsPerPage: perPage, filter}));

  const onProposeChanges = (added: string[], removed: string[]) => {
    setOpen(false);
    dispatch(proposeChanges({registryId: id || '', added, removed}));
  }

  const columns: Column<Entry>[] = [
    {id: "identity", label: localize('DID')},
    {id: "status", label: localize('Status')},
  ];

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
            <div style={{fontSize: 18, fontWeight: 600, paddingRight: 32}}>{localize('ENTRIES')}</div>
          </Breadcrumbs>
          <div>
            <HintIcon>
              <div dangerouslySetInnerHTML={localizedInnerHtml('HINTS_ENTRIES_LIST')}/>
            </HintIcon>
          </div>
        </AlignedHGroup>
        <AlignedHGroup>
          <div style={{paddingRight: 64}}>
            <SearchBar
              textFilter={filter}
              onTextFilterChange={onFilterChange}
            />
          </div>
          <div>
            <Button variant={'outlined'} startIcon={<AddIcon/>} onClick={() => setOpen(true)}>
              {localize('PROPOSE_CHANGE')}
            </Button>
          </div>
        </AlignedHGroup>
      </ContentHeader>
      <div style={{paddingTop: 16}}/>
      <Box sx={{width: 1, borderTop: '1px solid rgb(199, 199, 199)'}}>
        <div style={{paddingTop: 16}}/>
        {isLoading ?
          (<LoaderView/>) :
          (<TableList items={items} columns={columns}/>)
        }
      </Box>
      <div style={{paddingTop: 16}}/>
      <Paginator
        itemsPerPage={itemsPerPage}
        itemsTotal={itemsTotal}
        currentPage={currentPage}
        ranges={[6, 9, 15]}
        setPage={setPage}
        setItemsPerPage={setItemsPerPage}
      />
    </Box>
  );
};

export default EntriesList;
