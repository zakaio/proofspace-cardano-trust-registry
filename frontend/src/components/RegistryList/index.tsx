import {FC, useState} from "react";
import {Box, Button, IconButton, Table} from "@mui/material";
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
import {Registry} from "../../domain/Registry";
import {deleteRegistry, getRegistries, saveRegistry} from "../../app/state/registry";
import {useNavigate} from "react-router-dom";
import {createLink} from "../../app/cfg/config";
import {MainPath} from "../../app/cfg/RoutePath";
import EditRegistryPopUp from "./EditRegistryPopUp";
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import {WarningPopUp} from "../WarningPopUp";
import {getWorkAreaSizes} from "../../utils/domUtil";

const RegistryList: FC<{}> = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();

  const [selected, setSelected] = useState<Registry|undefined>();
  const [open, setOpen] = useState(false);
  const [warningOpen, setWarningOpen] = useState(false);


  const items = useAppSelector((state) => state.registry.items);
  const itemsTotal = useAppSelector((state) => state.registry.itemsTotal);
  const currentPage = useAppSelector((state) => state.registry.currentPage);
  const itemsPerPage = useAppSelector((state) => state.registry.itemsPerPage);
  const filter = useAppSelector((state) => state.registry.filter);
  const isLoading = useAppSelector((state) => state.registry.isLoading);

  useDidMount(() => dispatch(getRegistries({currentPage, itemsPerPage, filter})))

  const onFilterChange = (f: string) => dispatch(getRegistries({currentPage, itemsPerPage, filter: f}));
  const setPage = (page: number) => dispatch(getRegistries({currentPage: page, itemsPerPage, filter}));
  const setItemsPerPage = (perPage: number) => dispatch(getRegistries({currentPage, itemsPerPage: perPage, filter}));
  const onSelect = (item: Registry) => navigate(createLink(`${MainPath.REGISTRY}/${item.identity}`));

  const onSave = (item: Registry) => {
    setOpen(false);
    setSelected(undefined);
    dispatch(saveRegistry({item}));
  }

  const editHandler = (item: Registry) => (evt: any) => {
    evt.preventDefault();
    evt.stopPropagation();
    setSelected(item);
    setOpen(true);
  };

  const deleteHandler = (item: Registry) => (evt: any) => {
    evt.preventDefault();
    evt.stopPropagation();
    setSelected(item);
    setWarningOpen(true);
  };

  const confirmDelete = () => {
    setWarningOpen(false);
    if (selected) {
      dispatch(deleteRegistry({item: selected}));
    }
  };

  const columns: Column<Registry>[] = [
    {id: "name", label: localize('NAME')},
    {id: "schema", label: localize('SCHEMA')},
    {id: "network", label: localize('NETWORK')},
    {id: "subnetwork", label: localize('SUB_NETWORK')},
    // {id: "didPrefix", label: localize('DID_PREFIX')},
    /*{
      id: "identity",
      label: '',
      renderer: (item) => (
        <IconButton onClick={editHandler(item)}>
          <EditIcon/>
        </IconButton>
      )
    },*/
    {
      id: "identity",
      label: '',
      renderer: (item) => (
        <IconButton onClick={deleteHandler(item)}>
          <DeleteIcon/>
        </IconButton>
      )
    }
  ];

  const sizes = getWorkAreaSizes();

  return (
    <Box width={'100%'}>
      <EditRegistryPopUp open={open} item={selected} onCancel={() => setOpen(false)} onSave={onSave}/>
      <WarningPopUp
        opened={warningOpen}
        title={localize('DELETE_REGISTRY_WARNING')}
        message={localize('DELETE_REGISTRY_WARNING_MESSAGE')}
        onConfirm={confirmDelete}
        onCancel={() => setWarningOpen(false)}
      />
      <ContentHeader>
        <AlignedHGroup>
          <div style={{fontSize: 18, fontWeight: 600, paddingRight: 32}}>{localize('REGISTRY_LIST')}</div>
          <div>
            <HintIcon>
              <div dangerouslySetInnerHTML={localizedInnerHtml('HINTS_REGISTRY_LIST')}/>
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
            <Button
              variant={'outlined'}
              startIcon={<AddIcon/>}
              onClick={() => {
                setSelected(undefined);
                setOpen(true);
              }}
            >
              {localize('ADD_REGISTRY')}
            </Button>
          </div>
        </AlignedHGroup>
      </ContentHeader>
      <div style={{paddingTop: 16}}/>
      <Box sx={{width: 1, borderTop: '1px solid rgb(199, 199, 199)'}}>
        <div style={{paddingTop: 16}}/>
        {isLoading ?
          (<LoaderView/>) :
          (<TableList items={items} columns={columns} onSelect={onSelect} maxHeight={sizes.height - 64}/>)
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

export default RegistryList;
