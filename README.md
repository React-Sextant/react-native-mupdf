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

### startPDFActivity

|Options|Description|Example|
|----|----|----|
|OpenMode|打开模式String|"日常",主控方","被控方"||
|Uri|文件路径String|/0/Download/pdf_t1.pdf|
