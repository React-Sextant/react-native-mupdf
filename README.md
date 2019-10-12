# react-native-mupdf
mupdf-android-viewer in react native view manager

### Manual configuration
#### react-native/android/build.gradle
 - minSdkVersion > 16 `must`
 - compileSdkVersion = 28 `must`
 - targetSdkVersion = 22    `is best`
 - supportLibVersion = "28.0.0" `is best`

## Usage
```jsx harmony
import React from 'react'
import {View,requireNativeComponent} from 'react-native'

const Mupdf = requireNativeComponent("RCTMuPdf")

export default class extends React.Component {

    render(){
        return (
            <View style={{flex:1}}>
                <Mupdf
                    style={{flex:1}}
                    path={"/storage/emulated/0/Download/4.pdf"}
                />
            </View>
        )
    }
}

```
