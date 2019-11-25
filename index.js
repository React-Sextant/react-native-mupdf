import React from 'react'
import {View,requireNativeComponent} from 'react-native'

const MuPDF = requireNativeComponent("RCTMuPdf")
export default class Pdf extends React.Component {
    render(){
        return (
            <MuPDF {...this.props}/>
        )
    }
}
