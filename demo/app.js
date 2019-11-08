import React from 'react'
import {View,NativeModules,DeviceEventEmitter,Button} from 'react-native'

export default class extends React.Component {
    constructor(props){
        super(props);
        this.state={
            annotations:{},
            annotations2:{},
        }
    }

    componentDidMount() {
        this.setState({
            annotations2:JSON.parse("{\"0\":[{\"type\":\"add_markup_annotation\",\"path\":[[518.1655,981.7813],[955.01355,981.7813],[955.01355,961.856],[518.1655,961.856],[267.60016,1005.7813],[955.1838,1005.7813],[955.1838,985.856],[267.60016,985.856],[267.60016,1029.7814],[955.062,1029.7814],[955.062,1009.8561],[267.60016,1009.8561],[267.60016,1053.5415],[552.4896,1053.5415],[552.4896,1033.6162],[267.60016,1033.6162]],\"page\":0,\"annotation_type\":\"HIGHLIGHT\"}],\"1\":[{\"type\":\"add_markup_annotation\",\"path\":[[451.46677,904.1934],[954.9679,904.1934],[954.9679,882.89606],[451.46677,882.89606],[267.59982,928.1934],[934.57874,928.1934],[934.57874,906.89606],[267.59982,906.89606]],\"page\":1,\"annotation_type\":\"UNDERLINE\"}],\"2\":[{\"type\":\"add_markup_annotation\",\"path\":[[471.38556,681.06104],[955.27356,681.06104],[955.27356,661.13574],[471.38556,661.13574],[267.60114,704.8212],[955.0084,704.8212],[955.0084,684.89594],[267.60114,684.89594],[267.60114,728.8212],[955.0259,728.8212],[955.0259,708.89594],[267.60114,708.89594],[267.60114,752.8212],[493.44223,752.8212],[493.44223,732.89594],[267.60114,732.89594]],\"page\":2,\"annotation_type\":\"HIGHLIGHT\"}]}")
        });
    }

    openPDF=()=>{
        DeviceEventEmitter.addListener('MUPDF_Event_Manager',this.handleListenMuPDF,this);
        NativeModules.MuPDF.open({
            fileName:'pdf名称',
            filePath:'/storage/emulated/0/Download/pdf_t2.pdf'
        }).then(res=>{
            DeviceEventEmitter.removeAllListeners('MUPDF_Event_Manager',this.handleListenMuPDF,this);
        }).catch(err=>{
            DeviceEventEmitter.removeAllListeners('MUPDF_Event_Manager',this.handleListenMuPDF,this);
        })
    };

    handleListenMuPDF=(msg)=>{
        let data = JSON.parse(msg);

        let annotations = this.state.annotations||{};
        let annotations2 = this.state.annotations2||{};
        if(data.type === "add_annotation" || data.type === "add_markup_annotation"){
            if(Array.isArray(annotations[data.page])){
                annotations[data.page].push(data)
            }else {
                annotations[data.page] = [data]
            }
        }else if(data.type === "delete_annotation"){
            if(Array.isArray(annotations[data.page])){
                annotations[data.page].splice(data.annot_index,1);
            }
        }else if(data.type === "update_page"){
            if(Array.isArray(annotations2[data.page])){
                annotations2[data.page].forEach(a=>{
                    NativeModules.MuPDF.sendData(JSON.stringify(a))
                });
                annotations2[data.page] = [];
            }
        }

        this.setState({
            annotations:annotations,
            annotations2:annotations2,
        });

        console.log(data,annotations,annotations2)

    };

    uploadAnnotation=()=>{
        let annotations = this.state.annotations||{};
        let annotations2 = this.state.annotations2||{};
        for(let i in annotations2){
            if(Array.isArray(annotations2[i])&&annotations2[i].length>0){
                annotations[i] = annotations2[i]
            }
        }
    };


    render(){

        return (
            <View style={{flex:1}}>
    <Button title={"打开pdf"} onPress={this.openPDF}/>
        <View style={{flex:1}}/>
        </View>

    )
    }
}
