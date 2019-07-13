import {NativeModules} from 'react-native';

const { RNMuPdfModule } = NativeModules;

module.exports = {
    startPDFActivity(){
        RNMuPdfModule.startPDFActivity()
    }
};
