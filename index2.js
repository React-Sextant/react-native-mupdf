import {NativeModules, DeviceEventEmitter, AsyncStorage} from 'react-native';
import NetInfo from '@react-native-community/netinfo'
import Toast from "antd-mobile/lib/toast";
import RNFetchBlob from 'rn-fetch-blob'
import Progress from 'react-sextant/lib/root-view/progress'

const { MuPDF } = NativeModules;

let _isInMuPdf = false;        //是否在mupdf插件页内（只允许点一次文件）

/**
 * openMuPDF2
 *
 * @param {String} params.url                   文件在线地址
 * @param {String} params.title                 文件名称
 * @param {String} params.fileOtherRecordStr    文件批注数据
 * @param {String} params.md5                   文件md5用于对比新老文件
 * @param {Number} params.page                  初始页码（没有的话会默认mupdf cache page）
 * @param {Boolean} params.cache                文件是否允许被缓存
 * @param {Array} params.cacheList              缓存列表
 * @param {Array} params.menus                  MuPdf内按钮菜单
 * @param {Function} params.callback            成功打开MuPdf并关闭之后额度回调
 * @param {Function} params.onError             失败回调
 * @param {Function} params.onFinishActivityHook 关闭PDF钩子(返回键监听)
 * @param {Function} params.onLoadComplete      pdf加载已完成回调
 * **/
export async function openMuPDF2(params){
    if(_isInMuPdf){
        return false;
    }else {
        Progress.setLoading(0.01);
        let cache_list = params.cacheList || JSON.parse(await AsyncStorage.getItem('mupdf_file_data_path')||"[]");
        let index = cache_list.findIndex(pre=>{return Boolean(pre.md5===(params.md5||params.url))});
        if(index>-1) {
            Progress.setLoading(1);
            openMuPDF(cache_list[index].path||cache_list[index].filePath||cache_list[index].localPath,params.title,JSON.parse(params.fileOtherRecordStr||"{}"),params).then(res=>{
                typeof params.callback === 'function'&&params.callback(res)
            }).catch(err=>{
                typeof params.onError === 'function'&&params.onError(err)
            })
        }else {
            downloadFileFetch(params,(path)=>{
                openMuPDF(path,params.title,JSON.parse(params.fileOtherRecordStr||"{}"),params).then(res=>{
                    if(params.cache && !Array.isArray(params.cacheList)){
                        cache_list.push({
                            filePath:path,
                            md5:(params.md5||params.url)
                        });
                        AsyncStorage.setItem('mupdf_file_data_path',JSON.stringify(cache_list));
                    }
                    typeof params.callback === 'function'&&params.callback({...res,path})
                }).catch(async err=>{
                    await deleteLocationFile(path);
                    typeof params.onError === 'function'&&params.onError(err)
                })
            },params.onError)
        }
    }
}

export function openMuPDF(_filePath,_fileName,_annotations,_params={}){
    if(_isInMuPdf){
        return false;
    }else {
        _isInMuPdf = true;
        global.annotations = {};    //当前pdf产生的临时数据
        global.annotations2 = _annotations.annotations ? _annotations.annotations : _annotations;   //服务器拉取的数据
        DeviceEventEmitter.addListener('MUPDF_Event_Manager',(msg)=>handleListenMuPDF(msg,_params),this);
        return new Promise((resolve,reject) => {
            MuPDF.open({
                filePath:_filePath,
                fileName:_fileName,
                cloudData:_annotations.cloudData,
                menus:JSON.stringify(_params.menus)||"[{name:\"批注\"}]",
                theme:_params.theme||"",
                page:_params.page >= 0 ? _params.page : undefined
            }).then(res=>{
                Progress.setLoading(0);
                DeviceEventEmitter.removeAllListeners('MUPDF_Event_Manager');
                for(let i in annotations2){
                    if(Array.isArray(annotations2[i])&&annotations2[i].length>0){
                        annotations[i] = annotations2[i]
                    }
                }
                _isInMuPdf = false;
                _forbidden = false;
                resolve({...res,annotations:annotations})
            }).catch(err=>{
                Progress.setLoading(0);
                DeviceEventEmitter.removeAllListeners('MUPDF_Event_Manager');
                _isInMuPdf = false;
                _forbidden = false;
                reject(err)
            })
        })
    }
}
export function finishPDFActivity(){
    if(_isInMuPdf){
        sendData(JSON.stringify({
            type:"finish_activity"
        }))
    }
}
export function sendData(args){
    if(_isInMuPdf && typeof args === "string"){
        MuPDF.sendData(args)
    }
}
/**
 * 下载文件
 * **/
let mupdf_unsubscribe = undefined;
export function downloadFileFetch(params,callback,errorBack){
    try{
        mupdf_unsubscribe = NetInfo.addEventListener(handleConnectivityChange);
        Progress.setLoading(0.01);
        let task = RNFetchBlob.config({
            fileCache: true,
            appendExt: params.url.indexOf(".tif")>-1?'tif':'pdf'
        }).fetch('GET', params.url,params.headers);
        task.progress((received, total) => {
            Toast.hide();
            Progress.setLoading(Number(received / total).toFixed(2)*1);
        })
            .then(async (resp) => {
                if (resp.respInfo&&resp.respInfo.status !== 200) {
                    Toast.offline("文件"+(resp.respInfo?resp.respInfo.status:"文件信息有误"));
                    await deleteLocationFile(resp.path());
                    errorBack(resp.respInfo.status)
                }else {
                    callback(resp.path())
                }
                catchError();
            })
            .catch((err) => {
                catchError(errorBack,err)
            });


        //主动结束下载
        DeviceEventEmitter.addListener('fetch_download',()=>{
            if(task&&task.cancel){
                task.cancel(()=>{
                    catchError(errorBack,"主动结束下载")
                })
            }
        });

        //检测当前网络
        NetInfo.fetch().then((connectionInfo) => {
            if(connectionInfo.type==='none'){
                handleConnectivityChange()
            }
        })
    }catch (e) {
        catchError(errorBack,e)
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

let _page = 0;
let _forbidden = false;
export function handleListenMuPDF(msg,params){
    try{
        msg = msg.replace(/\n/g,"\\n").replace(/\r/g,"\\r");
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
            _page = data.page;

            if(_forbidden){
                if(Array.isArray(annotations[_page])&&annotations[_page].length>0){
                    annotations[_page].forEach((a,i)=>{
                        setTimeout(()=>{
                            sendData(JSON.stringify({
                                type:"delete_annotation",
                                annot_index:0,
                                page:_page
                            }))
                        },40*i)
                    });
                    annotations2[data.page] = annotations[_page];
                    annotations[_page] = [];
                }
            }else {
                if(Array.isArray(annotations2[data.page])&&annotations2[_page].length>0){
                    annotations2[data.page].forEach((a,i)=>{
                        setTimeout(()=>{
                            sendData(JSON.stringify(a))
                        },40*i)
                    });
                    annotations2[data.page] = [];
                }
            }


        }else if(data.type === "dynamic_menus_button"){
            if(data.name === "隐藏批注"){
                _forbidden = true;

                sendData(JSON.stringify({
                    ...data,
                    menus:"[{name:\"显示批注\"}]"
                }));

                if(Array.isArray(annotations[_page])&&annotations[_page].length>0){
                    annotations[_page].forEach((a,i)=>{
                        setTimeout(()=>{
                            sendData(JSON.stringify({
                                type:"delete_annotation",
                                annot_index:0,
                                page:_page
                            }))
                        },30*i)
                    });
                    annotations2[_page] = annotations[_page];
                    annotations[_page] = [];
                }

            }else if(data.name === "显示批注"){
                _forbidden = false;

                sendData(JSON.stringify({
                    ...data,
                    menus:"[{name:\"批注\"},{name:\"隐藏批注\"}]"
                }));

                if(Array.isArray(annotations2[_page])&&annotations2[_page].length>0){
                    annotations2[_page].forEach((a,i)=>{
                        setTimeout(()=>{
                            sendData(JSON.stringify(a))
                        },30*i)
                    });
                    annotations2[_page] = [];
                }
            }
        }else if(data.type === "on_load_complete"){
            if(typeof params.onLoadComplete === 'function'){
                setTimeout(()=>{
                    params.onLoadComplete()
                },500)
            }
        }else if(data.type === "on_finish_activity_hook"){
            if(typeof params.onFinishActivityHook === 'function'){
                params.onFinishActivityHook()
            }else {
                finishPDFActivity()
            }
        }
    }catch (e) {

    }
}

function catchError(errorBack,err){
    Progress.setLoading(0);
    typeof mupdf_unsubscribe === "function"&&mupdf_unsubscribe()
    DeviceEventEmitter.removeAllListeners('fetch_download');
    if(typeof errorBack === "function"){
        _isInMuPdf = false;
        errorBack(err)
    }
}

function handleConnectivityChange(){
    if(DeviceEventEmitter.listeners("fetch_download").length>0){
        DeviceEventEmitter.emit("fetch_download");
    }
}
