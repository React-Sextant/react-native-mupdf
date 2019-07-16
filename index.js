import {NativeModules} from 'react-native';

const { RNMuPdfModule } = NativeModules;

module.exports = {
    startPDFActivity(args){
        RNMuPdfModule.startPDFActivity(args)
    },
    finishPDFActivity(){
        RNMuPdfModule.finishPDFActivity()
    },
    sendData(args){
        RNMuPdfModule.sendData(args)
    },
};
