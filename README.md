# react-native-mupdf
Replace youkan pdf

### Manual configuration
#### react-native/android/build.gradle
 - minSdkVersion > 16 `must`
 - compileSdkVersion = 28 `must`
 - targetSdkVersion = 22    `is best`
 - supportLibVersion = "28.0.0" `is best`

## Usage

#### openMuPDF2
```jsx harmony
/**
 * params
 * @param params.url        String      （必填）文件在线地址
 * @param params.title      String      （必填）文件名称
 * @param params.fileOtherRecordStr      String 文件批注数据
 * @param params.md5        String      文件md5用于对比新老文件
 * @param params.cache      Boolean     文件是否允许被缓存
 * @param params.cacheList  Array       缓存列表
 * @param params.menus      Array       MuPdf内按钮菜单
 * @param params.callback   Function    成功打开MuPdf并关闭之后额度回调
 * @param params.onError    Function    失败回调
 * **/
onPress=async ()=>{
    await openMuPDF2({
        url:"",
        title:"",
        md5:"",
        cache:true,
        callback:()=>{
            DeviceEventEmitter.emit('mupdf_file_saved');
        },
        onError:()=>{

        }
    })
};
```

#### openMuPDF
```jsx harmony
import {AsyncStorage} from 'react-native'
import { downloadFileFetch, openMuPDF, deleteLocationFile } from 'react-native-mupdf'
import Progress from 'react-sextant/lib/root-view/progress'

async function open(){
    try{
        Progress.setLoading(0.01);
        let cache_list = JSON.parse(await AsyncStorage.getItem('mupdf_file_data_path')||"[]");
        let index = cache_list.findIndex(pre=>{return Boolean(pre.fileId===fileId&&Boolean(!fileMD5||pre.fileMD5===fileMD5))});
        if(index>-1){
            Progress.setLoading(1);
            openMuPDF(cache_list[index].filePath,title,JSON.parse(fileOtherRecordStr||"{}")).then(res=>{
                updateFileAnnotation(fileUUID,JSON.stringify(res));
            }).catch(err=>{
                deleteLocationFile(cache_list[index].filePath);
                cache_list.splice(index,1);
                AsyncStorage.setItem('mupdf_file_data_path',JSON.stringify(cache_list))
            })
        }else {
            downloadFileFetch({url:url},(path)=>{
                openMuPDF(path,title,JSON.parse(fileOtherRecordStr||"{}")).then(res=>{
                    updateFileAnnotation(fileUUID,JSON.stringify(res));
                    cache_list.push({
                        filePath:path,
                        fileId:fileId,
                        fileMD5:fileMD5
                    });
                    AsyncStorage.setItem('mupdf_file_data_path',JSON.stringify(cache_list))
                }).catch(err=>{
                    deleteLocationFile(path)
                })
            },()=>{
                Progress.setLoading(0);
            })
        }
    }catch (e) {
        Toast.fail("当前网络忙")
    }
}

function updateFileAnnotation(){}

```
