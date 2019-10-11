# react-native-mupdf
mupdf-android-viewer-mini in react native view manager

## Manual configuration
android/build.gradle
```bash
allprojects {
	repositories {
	    ...
	    maven { url 'http://maven.ghostscript.com/' } //add here
	}
}
```

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
