import React, {Component} from 'react';
import {View, DeviceEventEmitter, Button} from 'react-native'
import {startPDFActivity,sendData} from 'react-native-mupdf'

let _annotation = {};
export default class App extends Component {
    constructor(props){
        super(props);
        this.state={
            annotation:{
                0:['{"type":"add_annotation", "path":[[[243.77928,113.93964],[258.66016,113.93964],[304.97446,113.93964],[352.28793,115.40798],[382.25043,117.63096],[395.1787,117.87323],[399.6767,117.87323]],[[258.33615,160.31464],[262.31503,160.31464],[301.45062,160.84286],[348.1036,161.60858],[373.4365,161.60858],[383.87363,160.31464],[385.53098,156.84146]],[[268.9465,76.82929],[271.36218,80.84404],[273.85876,94.34292],[278.20496,109.90298],[281.60587,133.08678],[286.18832,163.62714],[292.10812,190.07538],[296.6886,211.79597],[298.09262,219.60805],[300.7452,219.88786]],[[349.78574,80.814644],[346.64743,98.8731],[341.08682,134.12965],[334.7656,182.73915],[332.5439,220.81538],[332.5439,231.64276]]],"page":0}'],
                2:[
                    '{"type":"add_markup_annotation", "path":[[138.89752,520.5388],[935.46967,520.5388],[935.46967,499.36597],[138.89752,499.36597],[102.08764,544.53284],[935.6316,544.53284],[935.6316,523.36],[102.08764,523.36],[102.249565,568.52686],[935.70337,568.52686],[935.70337,547.354],[102.249565,547.354],[102.32134,592.5209],[935.883,592.5209],[935.883,571.348],[102.32134,571.348]],"page":2,"annotation_type": "UNDERLINE"}'
                    ,'{"type":"add_markup_annotation", "path":[[313.24854,804.77405],[570.2165,804.77405],[570.2165,783.6012],[313.24854,783.6012],[313.24854,834.78],[529.24854,834.78],[529.24854,814.10815],[313.24854,814.10815],[313.24854,864.786],[457.24854,864.786],[457.24854,844.11414],[313.24854,844.11414],[289.05112,801.5708],[301.8511,801.5708],[301.8511,785.5708],[289.05112,785.5708]],"page":2,"annotation_type": "HIGHLIGHT"}'
                ]
            }
        };
    }

    componentDidMount(){
        _annotation = JSON.parse(JSON.stringify(this.state.annotation));
        DeviceEventEmitter.addListener('MUPDF_Event_Manager',this.handleListenAnnotation,this)
    }

    componentWillUnmount(){
        DeviceEventEmitter.removeListener('MUPDF_Event_Manager',this.handleListenAnnotation)
    }

    handleListenAnnotation=(msg)=>{
        let data = JSON.parse(msg);
        let {annotation} = this.state;
        if(typeof data.page === "number"){
            if(data.type === "add_annotation" || data.type === "add_markup_annotation"){
                if(Array.isArray(annotation[data.page])){
                    annotation[data.page].push(msg)
                }else {
                    annotation[data.page] = [];
                    annotation[data.page].push(msg)
                }
            }else if(data.type === "delete_annotation"){
                if(Array.isArray(annotation[data.page])&&annotation[data.page].length>0){
                    annotation[data.page].splice(data.annot_index, 1)
                }
            }else if(data.type === "update_page"){
                if(Array.isArray(_annotation[data.page])){
                    _annotation[data.page].forEach((a,i)=>{
                        sendData(_annotation[data.page][i])
                    });
                    _annotation[data.page] = undefined;
                }

            }
        }
        this.setState({annotation:annotation});
        console.log("a",msg,annotation,_annotation)
    };

    go=()=>{
        startPDFActivity({
            OpenMode:"日常",
            // OpenMode:"主控方",
            Uri:"/storage/emulated/0/Download/pdf_t1.pdf",
            Page:2
        });
    };

    render() {
        return (
            <View style={{flex:1,justifyContent: 'center'}}>
    <Button
        title={"launch"}
        onPress={this.go}
        />
        </View>
    );
    }
}
