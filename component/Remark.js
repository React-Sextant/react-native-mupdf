/* 备注 */

import React from 'react'
import {View,Button} from 'react-native'
import Modal from 'react-sextant/lib/modal/Modal'
import {sendData} from '../index'

class Remark extends React.Component {
    handlePress=()=>{
        console.log(this.props.data.base64)
        sendData(JSON.stringify({
            ...this.props.data,
            type:"confirm_remark_annotation"
        }));
        this.refs._modal.close()
    }

    handleCancel=()=>{
        sendData(JSON.stringify({
            type:"cancel_remark_annotation"
        }));
        this.refs._modal.close()
    }

    render(){
        return (
            <Modal visible ref={"_modal"} >
                <View style={{width:300,height:300,backgroundColor:'yellow'}}></View>
                <View style={{flexDirection:'row'}}>
                    <Button title={"提交"} onPress={this.handlePress}/>
                    <Button title={"取消"} onPress={this.handleCancel}/>
                </View>
            </Modal>
        )
    }
}

export default Remark;
