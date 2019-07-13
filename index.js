import {NativeModules} from 'react-native';

const { RNMuPdfModule } = NativeModules;
console.log(NativeModules)
module.exports = {
    startPDFActivity(){
        RNMuPdfModule.startPDFActivity()
    },
    finishPDFActivity(){
        RNMuPdfModule.finishPDFActivity()
    },
    sendData(){
        RNMuPdfModule.sendData()
    }
};
