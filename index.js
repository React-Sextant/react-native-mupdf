import {NativeModules} from 'react-native';

const { RNMuPdfModule } = NativeModules;

module.exports = {
    startPDFActivity(args){
        return new Promise((resolve,reject) => {
            RNMuPdfModule.startPDFActivity(args).then(res=>{
                resolve(res)
            }).catch(err=>{
                reject(err)
            })
        })

    },
    finishPDFActivity(){
        RNMuPdfModule.finishPDFActivity()
    },
    sendData(args){
        RNMuPdfModule.sendData(args)
    },
};
