import {FC, useEffect, useState} from "react";
import {useAppDispatch, useAppSelector, useDidMount, useWillUnmount} from "../../app/hooks";
import {LoaderView} from "../LoaderView";
import {Box, Button, Card, CardContent, CardHeader, Stack, Typography} from "@mui/material";
import { QRCode } from 'react-qrcode-logo';
import * as React from "react";
import {BrowserView, isAndroid, isIOS, MobileView} from 'react-device-detect';
import {localize} from "../../app/cfg/Language";
import logoBig from "../../LogoHuge.png";
// import logoM from "../../LogoBig.png";
import qrLogo from "../../LogoPS.png";
import ReplayIcon from '@mui/icons-material/Replay';
import KeyIcon from '@mui/icons-material/Key';
import WarningIcon from '@mui/icons-material/Warning';
import './index.css';

const expireTime =  30 * 60000;

const Auth: FC<{}> = () => {
  const [time, setTime] = useState('00:00');
  const [expired, setExpired] = useState(false);
  const dispatch = useAppDispatch();
  const isLoading = useAppSelector((state) => state.auth.isQrAuthLoading);
  const qrUrl = useAppSelector((state) => state.auth.qrUrl);
  const when = useAppSelector((state) => state.auth.when);

  useEffect(() => {
    const id = setInterval(() => {
      // console.log('interval', when);
      if (!when || expired) {
        return;
      }
      const expiresAt = (when || Date.now()) + expireTime;
      const secondsLeft = Math.round((expiresAt - Date.now())/1000);
      // console.log('seconds left', secondsLeft);
      if (secondsLeft > 0) {
        const mins = Math.floor(secondsLeft / 60);
        const seconds = secondsLeft % 60;
        const minsStr = mins < 10 ? `0${mins}` : mins;
        const secondsStr = seconds < 10 ? `0${seconds}` : seconds;
        setTime(`${minsStr}:${secondsStr}`);
      } else {
        console.log('qr code expired');
        clearInterval(id);
        setExpired(true);
      }
    }, 1000);
    return () => clearInterval(id);
  }, [when]);

  useDidMount(() => {
    console.log('i try to get QR auth');
  });

  /*useEffect(() => {
    updateQrCode();
  }, [qrUrl, expired]);*/

  /*const updateQrCode = async () => {
    console.log('try to update qr code');
    if (!qrUrl || expired) {
      return;
    }
    await wait(10);
    const el = document.getElementById(`authQr`);
    if (!el) {
      return;
    }
    await QRCode.toCanvas(el, qrUrl, {width: 500});
  }*/

  const tryAgain = () => {
    setExpired(false);
  };

  if (isLoading) {
    return (<LoaderView/>);
  }

  return (
    <Box width={'100%'} height={'100vh'} display={'flex'} flexDirection={'row'}>
      <MobileView>
        <Box width={'100%'} height={'100vh'} style={{backgroundColor: '#f3f2fe'}} position={'relative'} display={'flex'} alignItems={'center'}>
          <Box sx={{p: 3}} position={'absolute'} top={'0px'}>
            <div style={{paddingBottom: 32}}>
              <img src={logoBig} alt="ProofSpace logo" width={250}/>
            </div>
          </Box>
          <Box sx={{p: 3}}>
            <div>
              <Card style={{boxShadow: 'none', backgroundColor: '#f3f2fe', border: 'none', paddingLeft: 26, paddingRight: 26}}>
                <CardHeader
                  avatar={
                    <KeyIcon fontSize={'large'} color={'primary'}/>
                  }
                  title={
                    <Typography sx={{ fontSize: 16, fontWeight: 500 }} component="div">
                      { localize('AUTHORIZE_HEADER_TEXT')}
                    </Typography>
                  }
                />
                <CardContent>
                  <div style={{textAlign: 'left', fontSize: 14}}>
                    {localize('DESKTOP_ONLY_WARNING')}
                  </div>
                  <div style={{paddingTop: 32, textAlign: 'center', justifyContent: 'center'}}>
                    {isAndroid && (
                      <div>
                        <a href="https://play.google.com/store/apps/details?id=io.zaka.app" target="_blank" className="market-btn google-btn" role="button">
                          <span className="market-button-subtitle">Download on the</span>
                          <span className="market-button-title">Google Play</span>
                        </a>
                      </div>
                    )}
                    {isIOS && (
                      <div>
                        <a href="https://apps.apple.com/app/proofspace/id1512258409" target="_blank" className="market-btn apple-btn" role="button">
                          <span className="market-button-subtitle">Download on the</span>
                          <span className="market-button-title">App Store</span>
                        </a>
                      </div>
                    )}
                  </div>
                </CardContent>
              </Card>
            </div>
          </Box>
          <Box sx={{p: 3}} position={'absolute'} bottom={'0px'}>
            <div className={'bottomContacts bottomContactsMobile'}>
              Support: <a href={'mailto:team@proofspace.id'}>team@proofspace.id</a> | <a href={'https://twitter.com/proof_space'} target={'_blank'}>Twitter</a> | <a href={'https://www.youtube.com/channel/UClrbCIizVKCbT_qQfoD176g'} target={'_blank'}>YouTube</a> | <a href={'https://www.linkedin.com/company/joinproofspace/'} target={'_blank'}>LinkedIn</a>
            </div>
          </Box>
        </Box>
      </MobileView>

      <BrowserView>
        <Box width={'100%'} height={'100vh'} display={'flex'} flexDirection={'row'}>
        <Box width={'50%'} height={'100vh'} style={{backgroundColor: '#f3f2fe' /*#f7f7f7*/}} position={'relative'} display={'flex'} alignItems={'center'}>
          <Box sx={{p: 3}} position={'absolute'} top={'0px'}>
            <div style={{paddingBottom: 32}}>
              <img src={logoBig} alt="ProofSpace logo" width={250}/>
              {/*<br/><img src={logoM} alt="ProofSpace logo"/>*/}
            </div>
          </Box>
          <Box sx={{p: 3}}>
            <div>
              <Card style={{boxShadow: 'none', backgroundColor: '#f3f2fe', border: 'none', paddingLeft: 26, paddingRight: 26}}>
                <CardHeader
                  avatar={
                    <KeyIcon fontSize={'large'} color={'primary'}/>
                  }
                  title={
                    <Typography sx={{ fontSize: 22, fontWeight: 500 }} component="div">
                      { localize('AUTHORIZE_HEADER_TEXT')}
                    </Typography>
                  }
                  // subheader="September 14, 2016"
                />
                <CardContent>
                  <div style={{textAlign: 'left', fontSize: 20}}>
                    {localize('INSTRUCTIONS_HEADER')}
                  </div>
                  {/*
                    <ol>
                    <li>{localize('INSTALL_APP')}</li>
                    <li>{localize('GET_EMAIL_STAMP_CRED')}</li>
                    <li>{localize('SCAN_QR_CODE')}</li>
                  </ol>
                  */}
                  <Stack direction={'row'} style={{paddingTop: 32}}>
                    <div style={{paddingRight: 8}}>
                      <a href="https://apps.apple.com/app/proofspace/id1512258409" target="_blank" className="market-btn apple-btn" role="button">
                        <span className="market-button-subtitle">Download on the</span>
                        <span className="market-button-title">App Store</span>
                      </a>
                    </div>
                    <div>
                      <a href="https://play.google.com/store/apps/details?id=io.zaka.app" target="_blank" className="market-btn google-btn" role="button">
                        <span className="market-button-subtitle">Download on the</span>
                        <span className="market-button-title">Google Play</span>
                      </a>
                    </div>
                  </Stack>
                  <div style={{
                    border: 'solid 1px #E51E13',
                    borderRadius: 5,
                    marginTop: 32,
                    padding: 16,
                    display: 'flex',
                    flexDirection: 'row',
                    alignItems: 'center'
                  }}>
                    <div style={{paddingRight: 16}}>
                      <WarningIcon fontSize={'large'} style={{color: '#F39B31'}}/>
                    </div>
                    <div>
                      {localize('FIRST_TIME_WARNING_TEXT')}
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>
          </Box>
          <Box sx={{p: 3}} position={'absolute'} bottom={'0px'}>
            <div className={'bottomContacts'}>
              Support: <a href={'mailto:team@proofspace.id'}>team@proofspace.id</a> | <a href={'https://twitter.com/proof_space'} target={'_blank'}>Twitter</a> | <a href={'https://www.youtube.com/channel/UClrbCIizVKCbT_qQfoD176g'} target={'_blank'}>YouTube</a> | <a href={'https://www.linkedin.com/company/joinproofspace/'} target={'_blank'}>LinkedIn</a>
            </div>
          </Box>
        </Box>
        <Box height={'100vh'} width={'50%'} style={{backgroundColor: '#FFFFFF'}} display={'flex'} alignItems={'center'}>
          <div style={{width: '100%', justifyContent: 'center', textAlign: 'center', paddingTop: 24}}>
            <div>
              {expired ? (
                <div>
                  <div style={{paddingBottom: 16}}>
                    {localize('QR_CODE_IS_EXPIRED')}
                  </div>
                  <div>
                    <Button variant={'outlined'} onClick={tryAgain} startIcon={(<ReplayIcon/>)}>
                      {localize('TRY_AGAIN')}
                    </Button>
                  </div>
                </div>
              ) : (
                <div>
                  <QRCode
                    value={qrUrl}
                    size={400}
                    eyeRadius={{inner: [0, 0, 0, 0], outer: [6, 0, 6, 0]}}
                    logoImage={qrLogo}
                    logoPadding={8}
                    bgColor={'#f3f2fe'}
                    qrStyle={"dots"}
                  />
                  {/*<canvas id={`authQr`} />*/}
                  <div>{localize('QR_EXPIRES_IN')} {time}</div>
                </div>
              )}
            </div>
          </div>
        </Box>
        </Box>
      </BrowserView>
    </Box>
  );
};

export default Auth;
