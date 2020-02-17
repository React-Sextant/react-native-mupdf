import {NativeModules,DeviceEventEmitter} from 'react-native';
import Toast from "antd-mobile/lib/toast";
import RNFetchBlob from 'rn-fetch-blob'
import Progress from 'react-sextant/lib/root-view/progress'

const { MuPDF } = NativeModules;

let _isInMuPdf = false;        //是否在mupdf插件页内（只允许点一次文件）

export function openMuPDF(_filePath,_fileName,_annotations){
    if(_isInMuPdf){
        return false;
    }else {
        _isInMuPdf = true;
        global.annotations = {};    //当前pdf产生的临时数据
        global.annotations2 = _annotations.annotations ? _annotations.annotations : _annotations;   //服务器拉取的数据
        DeviceEventEmitter.addListener('MUPDF_Event_Manager',handleListenMuPDF,this);
        return new Promise((resolve,reject) => {
            MuPDF.open({
                filePath:_filePath,
                fileName:_fileName,
                cloudData:_annotations.cloudData
            }).then(res=>{
                Progress.setLoading(0);
                DeviceEventEmitter.removeAllListeners('MUPDF_Event_Manager',handleListenMuPDF,this);
                for(let i in annotations2){
                    if(Array.isArray(annotations2[i])&&annotations2[i].length>0){
                        annotations[i] = annotations2[i]
                    }
                }
                _isInMuPdf = false;
                resolve({...res,annotations:annotations})
            }).catch(err=>{
                Progress.setLoading(0);
                DeviceEventEmitter.removeAllListeners('MUPDF_Event_Manager',handleListenMuPDF,this);
                _isInMuPdf = false;
                reject(err)
            })
        })
    }
}
export function finishPDFActivity(){
    if(_isInMuPdf){
        MuPDF.finishPDFActivity()
    }
}
export function sendData(args){
    if(_isInMuPdf){
        MuPDF.sendData(args)
    }
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
        }).fetch('GET', params.url,params.headers);
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
                    errorBack('文件错误')
                }else {
                    Progress.setLoading(0);
                    callback(resp.path())
                }
            })
            .catch((err) => {
                _isInMuPdf = false;
                DeviceEventEmitter.removeAllListeners('fetch_download');
                errorBack(err)
            });


        //主动结束下载
        DeviceEventEmitter.addListener('fetch_download',()=>{
            if(task&&task.cancel){
                task.cancel(()=>{
                    _isInMuPdf = false;
                    DeviceEventEmitter.removeAllListeners('fetch_download');
                    errorBack("主动结束下载")
                })
            }
        });
    }catch (e) {
        _isInMuPdf = false;
        DeviceEventEmitter.removeAllListeners('fetch_download');
        errorBack(e)
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
            annotations2[data.page].forEach((a,i)=>{
                setTimeout(()=>{
                    MuPDF.sendData(JSON.stringify(a))
                },50*i)
            });
            annotations2[data.page] = [];
        }
    }
}
