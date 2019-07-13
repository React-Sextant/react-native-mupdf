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

## Usage
```jsx harmony
import {startPDFActivity} from 'react-native-mupdf'
```
