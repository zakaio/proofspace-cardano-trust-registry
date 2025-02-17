import {FC} from "react";
import {Box, Button, Card, CardActions, CardContent, CardMedia, Typography} from "@mui/material";
import {localize} from "../../app/cfg/Language";
import {useNavigate} from "react-router-dom";
import {createLink} from "../../app/cfg/config";
import {MainPath} from "../../app/cfg/RoutePath";
import DoDisturbOnIcon from '@mui/icons-material/DoDisturbOn';

const Denied: FC<{}> = () => {
  const navigate = useNavigate();
  return (
    <Box
      display="flex"
      justifyContent="center"
      alignItems="center"
      minHeight="70vh"
    >
      <Card>
        <CardMedia
          sx={{ height: 140, alignItems: 'center', justifyContent: 'center', display: 'flex' }}
        >
          <DoDisturbOnIcon style={{width: 120, height: 120, color: '#E51E13'}}/>
        </CardMedia>
        <CardContent>
          <Typography gutterBottom variant="h5" component="div">
            {localize('ACCESS_DENIED_HEADER')}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {localize('ACCESS_DENIED_DESCRIPTION')}
          </Typography>
        </CardContent>
        <CardActions sx={{justifyContent: 'center', padding: 2}}>
          <Button
            variant={'contained'}
            onClick={() => navigate(createLink(MainPath.ALL))}
          >
            {localize('TO_MY_ACCESS')}
          </Button>
        </CardActions>
      </Card>
    </Box>
  );
}

export default Denied;
