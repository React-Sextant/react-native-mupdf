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
                    size: 49.625,
                    page: jsonObj.Comments.Comment._PageNum*1,
                    text: jsonObj.Comments.Comment._PutText,
                    width: jsonObj.Comments.Comment._LineWidth,
                    height: 67,
                    x: cdata.Annotation.Point[0].X,
                    y: cdata.Annotation.Point[0].Y
                })
            }else if(jsonObj.Comments.Comment._SelText){
                const cdata = x2js.xml2js( "<Annotation>"+jsonObj.Comments.Comment.__cdata+"</Annotation>" );
                let _arr = [];
                cdata.Annotation.Point.forEach(p=>{
                    _arr.push([Number(p.X),Number(p.Y)])
                });
                Array.isArray(mupdf_annotation.annotations[a.pageNum])?
                    mupdf_annotation.annotations[a.pageNum].push({
                        type: "add_markup_annotation",
                        path: _arr,
                        page: a.pageNum
                    }):mupdf_annotation.annotations[a.pageNum]=[{
                        type: "add_markup_annotation",
                        path: _arr,
                        page: a.pageNum
                    }]
            }else if(!jsonObj.Comments.Comment._SelText&&!jsonObj.Comments.Comment._PutText){
                const cdata = x2js.xml2js( "<Annotation>"+jsonObj.Comments.Comment.__cdata+"</Annotation>" );
                let _annotations=[], _arr = [];
                cdata.Annotation.Point.forEach(p=>{
                    if(Number(p.X)===-1){
                        _annotations.push(_arr);
                        _arr = []
                    }else {
                        _arr.push([Number(p.X),Number(p.Y)])
                    }
                });

                if(_annotations.length===0){
                    cdata.Annotation.Point.forEach(p=>{
                        _annotations.push([Number(p.X),Number(p.Y)])
                    })
                }

                Array.isArray(mupdf_annotation.annotations[a.pageNum])?
                    mupdf_annotation.annotations[a.pageNum].push({
                        type: "add_annotation",
                        path: _annotations,
                        page: a.pageNum
                    }):mupdf_annotation.annotations[a.pageNum]=[{
                        type: "add_annotation",
                        path: _annotations,
                        page: a.pageNum
                    }]
            }
        });
        mupdf_annotation.cloudData = JSON.stringify(mupdf_annotation.cloudData);
        return mupdf_annotation;
    }catch (e) {
        return {"cloudData":str&&str.cloudData||"[]","annotations":str&&str.annotations||{}}
    }
}
