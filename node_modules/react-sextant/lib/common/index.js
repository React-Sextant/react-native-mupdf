import {Dimensions, PixelRatio} from "react-native";

/**
 * ui设计基准,iphone 6 2倍图
 * width:750px
 * height:1334px
 *
 * @description pad 1080
 * @description phone 375
 *
 */
export function scaleSize(size,padSize) {
    let screenW = Dimensions.get('window').width>Dimensions.get('window').height?Dimensions.get('window').height:Dimensions.get('window').width;
    const phone = screenW<330?330:375;
    const pad = 1080;
    if(padSize){
        return screenW<517?size * screenW / phone:padSize * screenW / pad
    }else {
        return screenW<517?size * screenW / phone:size * 2 * screenW / pad
    }
}
