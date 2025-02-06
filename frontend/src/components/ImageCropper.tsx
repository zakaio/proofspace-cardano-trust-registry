import PreviewIcon from '@mui/icons-material/InsertPhoto';
import ReactCrop, {centerCrop, Crop, makeAspectCrop, PercentCrop} from "react-image-crop";
import {FC, useEffect, useRef, useState} from "react";

import 'react-image-crop/dist/ReactCrop.css'

function centerAspectCrop(
  mediaWidth: number,
  mediaHeight: number,
  aspect: number,
) {
  return centerCrop(
    makeAspectCrop(
      {
        unit: '%',
        width: 90,
      },
      aspect,
      mediaWidth,
      mediaHeight,
    ),
    mediaWidth,
    mediaHeight,
  )
}

interface Props {
  source?: string;
  width: number;
  height: number;
  fileName: string;
  keepOriginSize?: boolean;
  onDone: (result: File) => void;
}

const ImageCropper: FC<Props> = ({
  source,
  width,
  height,
  fileName,
  keepOriginSize,
  onDone
}) => {
  let imgJustLoaded = false;
  const imageRef = useRef<HTMLImageElement>(null);
  // const [imageRef, setImageRef] = useState<HTMLImageElement | undefined>(undefined);
  const [crop, setCrop] = useState<Crop>();
  const [padding, setPadding] = useState(0);

  useEffect(() => {
    setCrop({unit: 'px', width, height, x: 0, y: 0});
  }, [width, height]);

  const onImageLoaded = (evt: any) => {
    imgJustLoaded = true;
    const image: HTMLImageElement = evt.currentTarget;
    if (imageRef.current) {
      setPadding((width * 2 - imageRef.current.width) / 2)
    }
    const aspect = width / height;
    const c: Crop = {x: 0, y: 0, height: image.height, width: image.height * aspect, unit: 'px'};
    if (image.width <= image.height * aspect) {
      c.width = image.width;
      c.height = image.width / aspect;
    }
    setCrop(c);
    makeClientCrop(c);
  };

  const onCropComplete = (c: Crop) => {
    makeClientCrop(c);
  };

  const onCropChange = (c: Crop, percentCrop: PercentCrop) => {
    if (imgJustLoaded) {
      imgJustLoaded = false;
      return;
    }
    setCrop(c)
  };

  const makeClientCrop = async (c: Crop) => {
    if (imageRef.current && c.width && c.height) {
      const file = await getCroppedImg(imageRef.current, c);
      onDone(file);
    }
  }

  const getCroppedImg = async (image: HTMLImageElement, c: Crop) => {
    const canvas: HTMLCanvasElement = document.createElement('canvas');
    const scaleX = image.naturalWidth / image.width;
    const scaleY = image.naturalHeight / image.height;
    const w = keepOriginSize ? (c.width as number) * scaleX : c.width as number;
    const h = keepOriginSize ? (c.height as number) * scaleY: c.height as number;
    canvas.width = w
    canvas.height = h;
    const ctx = canvas.getContext('2d') as CanvasRenderingContext2D;

    ctx.drawImage(
      image,
      (c.x as number) * scaleX,
      (c.y as number) * scaleY,
      (c.width as number) * scaleX,
      (c.height as number) * scaleY,
      0,
      0,
      w,
      h
    );

    return new Promise<File>((resolve, reject) => {
      canvas.toBlob((blob) => {
        if (!blob) {
          //reject(new Error('Canvas is empty'));
          console.error('Canvas is empty');
          return;
        }
        (blob as any).name = fileName || 'img.png';
        console.log(blob.size);
        resolve(blob as File);
        /*window.URL.revokeObjectURL(this.fileUrl);
        this.fileUrl = window.URL.createObjectURL(blob);
        resolve(this.fileUrl);*/
      }, 'image/png');
    });
  }

  return (
    <div style={{width: width * 2, textAlign: 'center', backgroundColor: '#e3e3e3'}}>
      <div style={{display: 'grid', width: 'auto'}}>
        <div
          style={{
            height: height * 2,
            width: 'auto',
            textAlign: 'center',
            alignItems: 'center',
            display: 'flex',
            paddingLeft: padding
          }}
        >
          {source ?
            (
              <ReactCrop
                crop={crop}
                ruleOfThirds={false}
                locked={true}
                // onImageLoaded={this.onImageLoaded}
                onComplete={onCropComplete}
                onChange={onCropChange}
              >
                <img
                  ref={imageRef}
                  style={{maxWidth: width * 2, maxHeight: height * 2}}
                  src={source}
                  onLoad={onImageLoaded}
                  alt={''}
                />
              </ReactCrop>
            ) : (
              <PreviewIcon style={{width: width * 2, height: height * 2, color: 'rgb(97, 97, 104)'}}/>
            )
          }
        </div>
      </div>
    </div>
  );
}

export default ImageCropper;
