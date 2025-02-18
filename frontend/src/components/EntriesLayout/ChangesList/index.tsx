import {FC, useState} from "react";
import {Box, Breadcrumbs, Button, IconButton, Table} from "@mui/material";
import AddIcon from '@mui/icons-material/Add';
import ContentHeader from "../../ContentHeader";
import AlignedHGroup from "../../AlignedHGroup";
import {localize} from "../../../app/cfg/Language";
import {HintIcon, localizedInnerHtml} from "../../HintIcon";
import {useAppDispatch, useAppSelector, useDidMount} from "../../../app/hooks";
import SearchBar from "../../SearchBar";
import Paginator from "../../Paginator";
import {LoaderView} from "../../LoaderView";
import TableList, {Column} from "../../TableList";
import {Link, useParams} from "react-router-dom";
import {createLink} from "../../../app/cfg/config";
import {MainPath} from "../../../app/cfg/RoutePath";
import ThumbUpIcon from '@mui/icons-material/ThumbUp';
import ThumbDownIcon from '@mui/icons-material/ThumbDown';
import dateFormat from "dateformat";
import {getWorkAreaSizes} from "../../../utils/domUtil";
import {approveChanges, getChanges, rejectChanges} from "../../../app/state/proposedChanges";
import {ProposedChange} from "../../../domain/ProposedChange";

interface ProposedChangeProps {
  change: ProposedChange;
  onAprove: (change: ProposedChange) => void;
  onReject: (change: ProposedChange) => void;
}
const ProposedChangeRenderer: FC<ProposedChangeProps> = ({change, onAprove, onReject}) => {
  if (!change) {
    return (<div/>);
  }
  const approveHandler = (evt: any) => {
    evt.stopPropagation();
    evt.preventDefault();
    onAprove(change);
  };

  const rejectHandler = (evt: any) => {
    evt.stopPropagation();
    evt.preventDefault();
    onReject(change);
  };

  return (
    <div>
      <div>{change.status.type}</div>
      <div style={{color: '#BAB8B5', fontSize: 12}}>{dateFormat(change.changeDate, "yyyy-mm-dd\'T\'HH:MM:ss")}</div>
      <AlignedHGroup>
        <div>
          <IconButton onClick={approveHandler}>
            <ThumbUpIcon style={{color: '#26C446'}}/>
          </IconButton>
        </div>
        <div>
          <IconButton onClick={rejectHandler}>
            <ThumbDownIcon style={{color: '#E51E13'}}/>
          </IconButton>
        </div>
      </AlignedHGroup>
    </div>
  );
};

interface Props {
  registryId: string;
}

const ChangesList: FC<Props> = ({registryId}) => {
  const dispatch = useAppDispatch();
  const items = useAppSelector((state) => state.changes.items);
  const itemsTotal = useAppSelector((state) => state.changes.itemsTotal);
  const currentPage = useAppSelector((state) => state.changes.currentPage);
  const itemsPerPage = useAppSelector((state) => state.changes.itemsPerPage);
  const filter = useAppSelector((state) => state.changes.filter);
  const isLoading = useAppSelector((state) => state.changes.isLoading);

  useDidMount(() => dispatch(getChanges({registryId, filter, itemsPerPage, currentPage})));

  const onFilterChange = (f: string) => dispatch(getChanges({registryId, currentPage, itemsPerPage, filter: f}));
  const setPage = (page: number) => dispatch(getChanges({registryId, currentPage: page, itemsPerPage, filter}));
  const setItemsPerPage = (perPage: number) => dispatch(getChanges({registryId, currentPage, itemsPerPage: perPage, filter}));

  /*const onProposeChanges = (added: string[], removed: string[]) => {
    setOpen(false);
    dispatch(proposeChanges({registryId: id || '', added, removed}));
  }*/

  const onApprove = (change: ProposedChange) => dispatch(approveChanges({registryId, changeId: change.identity}));

  const onReject = (change: ProposedChange) => dispatch(rejectChanges({registryId, changeId: change.identity}));

  const columns: Column<ProposedChange>[] = [
    {id: "identity", label: localize('DID')},
    {
      id: "status",
      label: localize('Status'),
      renderer: (item) => (<div>{item.status ? item.status.type : 'unknown'}</div>)
    },
    {
      id: 'proposedChange',
      label: localize('PROPOSED_CHANGE'),
      renderer: (item) => (
        <ProposedChangeRenderer
          change={item.proposedChange}
          onAprove={onApprove}
          onReject={onReject}
        />
      )
    }
  ];

  const sizes = getWorkAreaSizes();

  return (
    <Box width={'100%'}>
      <div style={{paddingTop: 16}}/>
      {isLoading ?
        (<LoaderView/>) :
        (<TableList items={items} columns={columns} maxHeight={sizes.height - 64}/>)
      }
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

export default ChangesList;
