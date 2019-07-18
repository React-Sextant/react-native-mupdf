# react-native-mupdf
a react native pdf activity module

### Installation
```bash
yarn add git+https://git@github.com/react-sextant/react-native-mupdf.git
```

### Add it to your android project
```bash
react-native link react-native-mupdf
```

### Manual configuration
#### react-native/android/build.gradle
 - minSdkVersion > 16 `must`
 - compileSdkVersion = 28 `must`
 - targetSdkVersion = 22    `is best`
 - supportLibVersion = "28.0.0" `is best`

#### android/app/build.gradle
Local reference AAR/JAR
```
android{
    ...
    
    repositories {
        flatDir {
            dirs project(':react-native-mupdf').file('libs')
        }
    }
}
```

## Usage
```jsx harmony
import {startPDFActivity} from 'react-native-mupdf'

startPDFActivity({
    OpenMode:"",
    Uri:""
})
```

### startPDFActivity 打开PDF

|Options|Description|Example|
|----|----|----|
|OpenMode|打开模式String|"日常","主控方","被控方"||
|Uri|文件路径String|/storage/emulated/0/Download/pdf_t1.pdf|

### finishPDFActivity 主动关闭当前页面并回到RN页

```jsx harmony
finishPDFActivity()
```

### sendData JavaScript发送数据到Native层

|Type|Description|Example|
|----|----|----|
|"add_annotation"|新增批注事件|`{type:"add_annotation",path:PointF[][],page:0}`|
|"add_markup_annotation"|新增标注（如下划线、高亮）事件|`{type:"add_markup_annotation",path:PointF[],annotation_type:"UNDERLINE",page:0}`|
|"delete_annotation"|删除批注（包括标注）事件|`{type:"delete_annotation", annot_index:-1, page:0}`|
|"update_page"|更新页面事件|`{type:"update_page",page:0}`|

监听来自Native层的事件
```jsx harmony
DeviceEventEmitter.addListener('MUPDF_Event_Manager', (msg) => {
    sendData(msg)
});
```
