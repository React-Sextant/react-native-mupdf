import {NativeModules,DeviceEventEmitter} from 'react-native';
import Toast from "antd-mobile/lib/toast";
import RNFetchBlob from 'rn-fetch-blob'
import Progress from 'react-sextant/lib/root-view/progress'

const { MuPDF } = NativeModules;

global.once = false;        //只允许点一次文件

export function openMuPDF(_filePath,_fileName,_annotations){
    if(global.once){
        return false;
    }else {
        global.annotations = {};    //当前pdf产生的临时数据
        global.annotations2 = _annotations;   //服务器拉取的数据
        DeviceEventEmitter.addListener('MUPDF_Event_Manager',handleListenMuPDF,this);
        return new Promise((resolve,reject) => {
            MuPDF.open({
                filePath:_filePath,
                fileName:_fileName,
            }).then(res=>{
                DeviceEventEmitter.removeAllListeners('MUPDF_Event_Manager',handleListenMuPDF,this);
                for(let i in annotations2){
                    if(Array.isArray(annotations2[i])&&annotations2[i].length>0){
                        annotations[i] = annotations2[i]
                    }
                }
                global.once = false;
                resolve({...res,annotations:annotations})
            }).catch(err=>{
                DeviceEventEmitter.removeAllListeners('MUPDF_Event_Manager',handleListenMuPDF,this);
                global.once = false;
                reject(err)
            })
        })
    }
}
export function finishPDFActivity(){
    MuPDF.finishPDFActivity()
}
export function sendData(args){
    MuPDF.sendData(args)
}
/**
 * 下载文件
 * **/
export function downloadFileFetch(params,callback,errorBack){
    try{
        let totalSize = 0;
        let task = RNFetchBlob.config({
            fileCache: true,
            appendExt: params.url.indexOf(".tif")>-1?'tif':'pdf'
        }).fetch('GET', params.url);
        task.progress((received, total) => {
            totalSize = total;
            Progress.setLoading(Number(received / total).toFixed(2)*1);
        })
            .then(async (resp) => {
                DeviceEventEmitter.removeAllListeners('fetch_download');
                let fileSize = await RNFetchBlob.fs.stat(resp.path());
                if (fileSize.size != totalSize || fileSize.size < 2000) {
                    Toast.offline('服务器繁忙，请重试！');
                    await deleteLocationFile(resp.path());
                    errorBack()
                }else {
                    callback(resp.path())
                }
            })
            .catch((err) => {
                global.once = false;
                DeviceEventEmitter.removeAllListeners('fetch_download');
                errorBack()
            });


        //主动结束下载
        DeviceEventEmitter.addListener('fetch_download',()=>{
            if(task&&task.cancel){
                task.cancel(()=>{
                    global.once = false;
                    DeviceEventEmitter.removeAllListeners('fetch_download');
                    errorBack()
                })
            }
        });
    }catch (e) {
        global.once = false;
        DeviceEventEmitter.removeAllListeners('fetch_download');
        errorBack()
    }
}

/**
 * 文件下载失败时清除缓存
 * **/
export function deleteLocationFile(path){
    return RNFetchBlob.fs.unlink(path).then(() => {
        return true
    });
}

export function handleListenMuPDF(msg){
    let data = JSON.parse(msg);
    if(data.type === "add_annotation" || data.type === "add_markup_annotation"){
        if(Array.isArray(annotations[data.page])){
            annotations[data.page].push(data)
        }else {
            annotations[data.page] = [data]
        }
    }else if(data.type === "delete_annotation"){
        if(Array.isArray(annotations[data.page])){
            annotations[data.page].splice(data.annot_index-1,1);
        }
    }else if(data.type === "update_page"){
        if(Array.isArray(annotations2[data.page])){
            annotations2[data.page].forEach(a=>{
                MuPDF.sendData(JSON.stringify(a))
            });
            annotations2[data.page] = [];
        }
    }
}
