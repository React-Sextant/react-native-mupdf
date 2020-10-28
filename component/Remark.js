/* 备注 */

import React from 'react'
import {View,Modal,Dimensions,Image,Text,ScrollView,StyleSheet,TextInput,TouchableOpacity} from 'react-native'
import {RootView} from 'react-sextant'
import Icon from 'react-native-vector-icons/AntDesign'
import {sendData} from '../index'

class Remark extends React.Component {
    state={
        isFocused:false,
        visible:true,
        value:"",
        width:Dimensions.get("window").width-80,
        height:100,
        data:[0]
    };

    componentWillMount() {
        Image.getSize("data:image/png;base64,"+this.props.data.base64, (width, height) => {
            this.setState({ height: height * (this.state.width / width) });
        });
        Dimensions.addEventListener('change', this.onConfigurationChanged);
    }

    componentWillUnmount() {
        Dimensions.removeEventListener('change',this.onConfigurationChanged);
    }

    onConfigurationChanged=(e)=>{
        const { width,height } = e.window;
        this.setState(function(preState){
            return {width:width-80}
        },()=>{
            Image.getSize("data:image/png;base64,"+this.props.data.base64, (width, height) => {
                this.setState({ height: height * (this.state.width / width) });
            });
        })
    }

    handlePress=()=>{
        sendData(JSON.stringify({
            ...this.props.data,
            type:"confirm_remark_annotation"
        }));
    }

    handleCancel=()=>{
        sendData(JSON.stringify({
            type:"cancel_remark_annotation"
        }));
        this.setState({visible:false},()=>{
            RootView.hide()
        })
    }

    submit=()=>{
        this.setState(function(preState){
            if(preState.value){
                this.handlePress();
                return {data:[...preState.data,preState.value]}
            }
        },()=>{
            this.setState({value:""});
            this.refs._TextInput&&this.refs._TextInput.blur()
        })
    }

    onFocus=()=>{
        this.setState({isFocused:true})
    };

    onBlur=()=>{
        this.setState({isFocused:false})
    };

    onChangeText=(text)=>{
        this.setState({value:text})
    }

    render(){
        const {isFocused,value,height,width,data} = this.state;
        return (
            <Modal visible={this.state.visible} transparent={true} >
                <View style={styles.bg}/>
                <ScrollView style={{marginTop:75}}>
                    <View style={[styles.card,{backgroundColor:'#FFFFFF'}]}>
                        <Image style={{ width,height:Math.min(height,110),alignSelf:'center' }} resizeMode={"contain"} source={{uri:"data:image/png;base64,"+this.props.data.base64}}/>
                    </View>
                    {data.map(a=>{
                        return (
                            <View style={styles.card} key={a}>
                                <Text style={styles.time}>
                                    2020年10月27日
                                </Text>
                                <Text style={styles.p}>
                                    {a?a:"这是一段测试备注{\"\\n\"}\n" +
                                        "                                    这是一段测试备注{\"\\n\"}\n" +
                                        "                                    这是一段测试备注{\"\\n\"}\n" +
                                        "                                    这是一段测试备注{\"\\n\"}\n" +
                                        "                                    这是一段测试备注{\"\\n\"}\n" +
                                        "                                    这是一段测试备注{\"\\n\"}\n" +
                                        "                                    这是一段测试备注{\"\\n\"}"}

                                </Text>
                            </View>
                        )
                    })}
                    <View style={{height:100}}/>
                </ScrollView>

                {!isFocused ?
                    <View style={[styles.footer, {padding: 20}]}>
                        <TouchableOpacity style={styles.close} onPress={this.handleCancel}>
                            <Icon name={'close'} color={'#FFFFFF'} size={20}/>
                        </TouchableOpacity>
                        <TouchableOpacity style={styles.footer_box2} onPress={this.onFocus}>
                            <View style={styles.input2}>
                                <Text style={{color:'#FFFFFF'}} numberOfLines={1}>
                                    {this.state.value||"备注"}
                                </Text>
                            </View>
                        </TouchableOpacity>
                    </View>
                    :
                    <View style={styles.footer}>
                        <View style={styles.footer_box}>
                            <View style={{alignSelf: 'flex-start'}}>
                                <Text style={{fontSize: 25, color: '#FFFFFF'}}>🏷️</Text>
                            </View>
                            <TextInput style={styles.input}
                                       ref={"_TextInput"}
                                       multiline
                                       autoFocus
                                       value={value}
                                       onBlur={this.onBlur}
                                       onChangeText={this.onChangeText}
                                       placeholder='备注'
                                       placeholderTextColor="#999999"
                                       underlineColorAndroid={"#FFFFFF"}
                            />
                            <TouchableOpacity style={{padding: 10}} onPress={this.submit}>
                                <Text style={[styles.time, {fontSize: 20,}]}>发表</Text>
                            </TouchableOpacity>
                        </View>
                    </View>
                }
            </Modal>
        )
    }
}

const styles = StyleSheet.create({
    bg:{
        position:'absolute',
        width:'100%',
        height:'100%',
        backgroundColor: '#000000',
        opacity:0.5,
    },
    card:{
        marginHorizontal:40,
        marginVertical:10,
        padding:20,
        borderRadius:10,
        backgroundColor:'#474747'
    },
    img:{
        width:'100%',
        height:150
    },
    time:{
        color:'#CCCCCC'
    },
    p:{
        color:'#FFFFFF'
    },
    input:{
        flex:1,
        color:'#FFFFFF',
        backgroundColor:'#474747',
        marginHorizontal:10
    },
    input2:{
        flex:1,
        justifyContent:'center',
        marginHorizontal:20,
        backgroundColor:'#474747',
        borderRadius: 50,
        paddingLeft:20
    },
    close:{
        justifyContent:'center',
        alignItems:'center',
        width:50,
        height:50,
        borderRadius:25,
        backgroundColor:'#474747'
    },
    footer:{
        flexDirection:'row',
        alignItems:'center',
        justifyContent:'center',
        position:'absolute',
        bottom:0,
        width:'100%',
        backgroundColor:'transparent'
    },
    footer_box:{
        backgroundColor:'#474747',
        width:'100%',
        flexDirection: 'row',
        alignItems:'flex-end',
        paddingTop:10
    },
    footer_box2:{
        flex:1,
        justifyContent:'center',
    },
});

export default Remark;
