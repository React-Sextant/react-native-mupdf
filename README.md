# react-native-mupdf
Replace youkan pdf

### Manual configuration
#### react-native/android/build.gradle
 - minSdkVersion > 16 `must`
 - compileSdkVersion = 28 `must`
 - targetSdkVersion = 22    `is best`
 - supportLibVersion = "28.0.0" `is best`

## Usage
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
                updateFileAnnotation(fileUUID,JSON.stringify(res.annotations));
            }).catch(err=>{
                deleteLocationFile(cache_list[index].filePath);
                cache_list.splice(index,1);
                AsyncStorage.setItem('mupdf_file_data_path',JSON.stringify(cache_list))
            })
        }else {
            downloadFileFetch({url:url},(path)=>{
                openMuPDF(path,title,JSON.parse(fileOtherRecordStr||"{}")).then(res=>{
                    updateFileAnnotation(fileUUID,JSON.stringify(res.annotations));
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
