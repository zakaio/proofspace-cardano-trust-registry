import * as React from 'react';
import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';

export interface Column<T> {
  id: keyof T;
  label: string;
  minWidth?: number;
  align?: 'right';
  renderer?: (item: T) => React.ReactNode;
}

interface Props<T> {
  items: T[];
  columns: Column<T>[];
  onSelect?: (item: T) => void;
}

const TableList = <T,>(props: Props<T>) => {
  const {items, columns, onSelect} = props;
  return (
    <Paper sx={{ width: '100%', overflow: 'hidden' }}>
      <TableContainer sx={{ maxHeight: 440 }}>
        <Table stickyHeader aria-label="sticky table">
          <TableHead>
            <TableRow>
              {columns.map((column, i) => (
                <TableCell
                  key={i}
                  align={column.align}
                  style={{ minWidth: column.minWidth }}
                >
                  {column.label}
                </TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {items
              .map((row, i) => {
                return (
                  <TableRow hover role="checkbox" tabIndex={-1} key={i} onClick={() => onSelect !== undefined && onSelect(row)}>
                    {columns.map((column, ci) => {
                      const value = row[column.id];
                      const render = () => {
                        if (column.renderer !== undefined) {
                          return column.renderer(row);
                        }
                        return value as string;
                      }

                      return (
                        <TableCell key={ci} align={column.align}>
                          {render()}
                        </TableCell>
                      );
                    })}
                  </TableRow>
                );
              })}
          </TableBody>
        </Table>
      </TableContainer>
    </Paper>
  );
}

export default TableList;
