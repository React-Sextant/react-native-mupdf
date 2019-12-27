import {View,StyleSheet, TextInput,Image,TouchableOpacity,Platform} from "react-native";
import React from "react";
import {scaleSize} from "../common";

export default class extends React.PureComponent{
    static defaultProps = {
        clearButtonMode:"while-editing",
        maxLength:50,
        onFocus:Function,
        onBlur:Function,
        onChangeText:Function,
        inputStyle:{}
    };

    constructor(props){
        super(props);
        this.state={
            visible:false
        };
    }

    clear=()=>{
        this._root.clear();
        this.props.onChangeText('');
        this.setState({visible:false})
    };

    onFocus=()=>{
        if(this.props.value) this.setState({visible:true});
        return this.props.onFocus&&this.props.onFocus()
    };
    onBlur=()=>{
        this.setState({visible:false})
    };

    onChangeText= (text)=>{
        this.props.onChangeText(text);
        if(text&&!this.state.visible){
            this.setState({visible:true})
        }else if(!text){
            this.setState({visible:false})
        }
    };

    render(){
        return (
            <View style={[styles.input_box,{...this.props.style}]}>
                <View>
                    {this.props.children}
                </View>
                <View style={{flex:1,flexDirection: 'row',alignItems: "center"}}>
                    <TextInput
                        {...{...this.props,children:null}}
                        ref={c => {
                            this._root = c
                        }}
                        onFocus={this.onFocus}
                        onBlur={this.onBlur}
                        onChangeText={this.onChangeText}
                        style={[styles.input,this.props.inputStyle]}
                    />
                    {Boolean(Platform.OS==='android'&&this.state.visible)&&<TouchableOpacity onPress={this.clear} style={{backgroundColor: 'transparent',zIndex:999,marginLeft:scaleSize(-30)}}>
                        <Image source={require('./close.png')} resizeMode={'contain'} style={{width:scaleSize(15)}}/>
                    </TouchableOpacity>}
                </View>
                <View>
                    {this.props.extra}
                </View>
            </View>
        )
    }

}

const styles = StyleSheet.create({
    input_box: {
        width:'100%',
        alignItems: 'center',
        flexDirection: 'row',
        backgroundColor:'white',
        justifyContent:'space-between'
    },
    input:{
        height: scaleSize(40),
        width:'100%',
        borderColor: '#EFEFEF',
        fontSize:scaleSize(14),
    }
});
