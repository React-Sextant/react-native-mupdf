import React from 'react'
import {requireNativeComponent, View} from 'react-native'
const Mupdf = requireNativeComponent("RCTMuPdf")
export default class extends React.Component {

    render(){
        return (
            <View style={{flex:1,justifyContent: 'center'}}>
                <Mupdf
                    style={{flex:1,backgroundColor:'red'}}
                />
            </View>
        )
    }
}
