/* 备注 */

import React from 'react'
import {View,Modal,Text,ScrollView,StyleSheet,TextInput,TouchableOpacity} from 'react-native'
import Icon from 'react-native-vector-icons/AntDesign'
class Remark extends React.Component {
    state={
        isFocused:true
    };
    handlePress=()=>{
        this.refs._modal.close()
    }

    handleCancel=()=>{
        this.refs._modal.close()
    }

    onFocus=()=>{
        this.setState({isFocused:true})
    };

    onBlur=()=>{
        this.setState({isFocused:false})
    };

    render(){
        const {isFocused} = this.state;
        return (
            <Modal visible={this.props.visible} transparent={true} ref={"_modal"} >
                <View style={styles.bg}/>
                <ScrollView>
                    {[0,1,3,2,2,2].map(a=>{
                        return (
                            <View style={styles.card} key={a}>
                                <Text style={styles.time}>
                                    2020年10月27日
                                </Text>
                                <Text style={styles.p}>
                                    这是一段测试备注{"\n"}
                                    这是一段测试备注{"\n"}
                                    这是一段测试备注{"\n"}
                                    这是一段测试备注{"\n"}
                                    这是一段测试备注{"\n"}
                                    这是一段测试备注{"\n"}
                                    这是一段测试备注{"\n"}
                                </Text>
                            </View>
                        )
                    })}
                    <View style={{height:100}}/>
                </ScrollView>
                <View style={[styles.footer,!isFocused&&{padding:20}]}>
                    {!isFocused&&
                    <TouchableOpacity style={styles.close}>
                        <Icon name={'close'} color={'#FFFFFF'} size={20}/>
                    </TouchableOpacity>
                    }
                    <View style={isFocused?styles.footer_box:styles.footer_box2}>
                        {isFocused &&
                        <View style={{alignSelf: 'flex-start'}}>
                            <Text style={{fontSize: 25, color: '#FFFFFF'}}>🏷️</Text>
                        </View>
                        }
                        <TextInput style={isFocused?styles.input:styles.input2}
                                   multiline
                                   onFocus={this.onFocus}
                                   onBlur={this.onBlur}
                                   placeholder='备注'
                                   placeholderTextColor="#999999"
                                   underlineColorAndroid={isFocused?"#FFFFFF":"transparent"}
                        />
                        {isFocused &&
                        <TouchableOpacity style={{padding: 10}}>
                            <Text style={[styles.time, {fontSize: 20,}]}>发表</Text>
                        </TouchableOpacity>
                        }
                    </View>
                </View>
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
        opacity:0.3,
    },
    card:{
        marginHorizontal:40,
        marginVertical:10,
        padding:20,
        borderRadius:10,
        backgroundColor:'#474747'
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
        color:'#FFFFFF',
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
