import X2JS from 'x2js'

export function annotationParse(str){
    try{
        var annotation = JSON.parse(str);
        const x2js = new X2JS();
        let mupdf_annotation = {"cloudData":[],"annotations":{}};
        annotation.forEach(a=>{
            const jsonObj = x2js.xml2js( a.context );
            if(jsonObj.Comments.Comment._PutText){
                const cdata = x2js.xml2js( "<Annotation>"+jsonObj.Comments.Comment.__cdata+"</Annotation>" );
                mupdf_annotation.cloudData.push({
                    size: 100/jsonObj.Comments.Comment._Zoom,
                    page: jsonObj.Comments.Comment._PageNum*1-1,
                    text: jsonObj.Comments.Comment._PutText,
                    width: jsonObj.Comments.Comment._LineWidth*12/jsonObj.Comments.Comment._Zoom,
                    height: jsonObj.Comments.Comment._PutText.split("\n").length * 120 / jsonObj.Comments.Comment._Zoom,
                    x: cdata.Annotation.Point[0].X*2,
                    y: cdata.Annotation.Point[0].Y*2
                })
            // }else if(jsonObj.Comments.Comment._SelText){
            //     const cdata = x2js.xml2js( "<Annotation>"+jsonObj.Comments.Comment.__cdata+"</Annotation>" );
            //     let _arr = [];
            //     cdata.Annotation.Point.forEach(p=>{
            //         _arr.push([Number(p.X),Number(p.Y)])
            //     });
            //     Array.isArray(mupdf_annotation.annotations[a.pageNum])?
            //         mupdf_annotation.annotations[a.pageNum-1].push({
            //             type: "add_markup_annotation",
            //             path: _arr,
            //             page: a.pageNum-1
            //         }):mupdf_annotation.annotations[a.pageNum-1]=[{
            //             type: "add_markup_annotation",
            //             path: _arr,
            //             page: a.pageNum-1
            //         }]
            }else if(!jsonObj.Comments.Comment._SelText&&!jsonObj.Comments.Comment._PutText){
                const cdata = x2js.xml2js( "<Annotation>"+jsonObj.Comments.Comment.__cdata+"</Annotation>" );
                let _annotations=[], _arr = [];
                cdata.Annotation.Point.forEach(p=>{
                    if(Number(p.X)===-1){
                        _annotations.push(_arr);
                        _arr = []
                    }else {
                        _arr.push([Number(p.X)*2,Number(p.Y)*2])
                    }
                });

                if(_annotations.length===0){
                    cdata.Annotation.Point.forEach(p=>{
                        _annotations.push([Number(p.X)*2,Number(p.Y)*2])
                    })
                }

                Array.isArray(mupdf_annotation.annotations[a.pageNum-1])?
                    mupdf_annotation.annotations[a.pageNum-1].push({
                        type: "add_annotation",
                        path: _annotations,
                        page: a.pageNum-1
                    }):mupdf_annotation.annotations[a.pageNum-1]=[{
                        type: "add_annotation",
                        path: _annotations,
                        page: a.pageNum-1
                    }]
            }
        });
        mupdf_annotation.cloudData = JSON.stringify(mupdf_annotation.cloudData);
        return mupdf_annotation;
    }catch (e) {
        try{
            return {"cloudData":JSON.parse(str).cloudData||"[]","annotations":JSON.parse(str).annotations||{}}
        }catch (e) {
            return {"cloudData":str&&str.cloudData||"[]","annotations":str&&str.annotations||{}}
        }
    }
}
