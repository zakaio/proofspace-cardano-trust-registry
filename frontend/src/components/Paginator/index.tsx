import {FC} from "react";
import Pagination from '@mui/material/Pagination';
import {FormControl, NativeSelect, Box} from "@mui/material";
import "./styles.css";

interface Props {
  itemsPerPage: number;
  itemsTotal: number;
  currentPage: number;
  ranges: number[];
  setPage: (page: number) => void;
  setItemsPerPage: (itemsPerPage: number) => void;
}

const CustomPagination: FC<Props> = ({
  itemsPerPage,
  itemsTotal,
  currentPage,
  ranges,
  setPage,
  setItemsPerPage
}) => {

  const numPages = Math.ceil(itemsTotal / itemsPerPage);
  const from = (currentPage - 1) * itemsPerPage + 1;
  const to = Math.min(itemsTotal, currentPage * itemsPerPage);

  const strRange = (...args: any[]) => {
    let str = "Showing {0}-{1} of {2}";

    if (args.length) {
      args.forEach((arg, index) => {
        str = str.replace(`{${index}}`, arg);
      });
    }
    return str;
  };

  return (
    <Box display="flex" alignItems="center">
      <Box sx={{paddingLeft: "15px"}}>{strRange(from, to, itemsTotal)}</Box>
      <FormControl className={'paginationFormControl'}>
        <NativeSelect
          value={itemsPerPage}
          onChange={(e) => setItemsPerPage(parseInt(e.target.value, 10))}
          name="pages"
        >
          {ranges.map((row, i) => (
            <option value={row} key={i}>
              {row}
            </option>
          ))}
        </NativeSelect>
      </FormControl>
      <Box flexGrow={1} />
      <Pagination count={numPages} onChange={(_e, page) => setPage(page)} page={currentPage} />
    </Box>
  );
};

export default CustomPagination;
