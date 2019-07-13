package com.github.react.sextant.util;

import android.content.Context;
import android.util.Log;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public class ParseNoteXml {
    private Context mcontext = null;
    private Document doc = null;
    private Element root = null;
    private Element firstWorldElement = null;

    public ParseNoteXml(Context context) {
        this.mcontext = context;
    }

    public void ParseNoteXmlData(String strConent) {
        Log.i("解析批注字符串",strConent);
        try {
            this.doc = DocumentHelper.parseText(strConent);
            this.root = this.doc.getRootElement();
            this.firstWorldElement = this.root.element("Comment");
        } catch (DocumentException var3) {
            var3.printStackTrace();
        }

    }

    public String GetAttributeValue(String strAttribute) {
        return this.firstWorldElement.attributeValue(strAttribute);
    }

    public void SetAttributeValue(String strAttribute, String strAttributeValue) {
        this.firstWorldElement.addAttribute(strAttribute, strAttributeValue);
    }

    public String GetXmlData() {
        return this.firstWorldElement.getData().toString();
    }

    public void SetXmlData(String strData) {
        this.firstWorldElement.addCDATA(strData);
    }

    public void ClearxmlData() {
        this.firstWorldElement.clearContent();
    }

    public String getxml() {
        return this.root.asXML();
    }

//    public void EditBookNotes(String strBookId, String strNotesId, String strBookDaoId, String strPageNum, String strParams, int EditType, int FileType) {
//        BookNoteDao mBookNoteDao = new BookNoteDao(this.mcontext);
//        List<Map<String, Object>> BookNoteList = new ArrayList();
//        if (FileType == 0) {
//            BookNoteList = mBookNoteDao.findPageNumList(strBookId, strPageNum);
//        } else if (FileType == 1) {
//            BookNoteList = mBookNoteDao.findist(strBookId);
//        }
//
//        if (BookNoteList != null && ((List)BookNoteList).size() > 0) {
//            new HashMap();
//
//            for(int i = 0; i < ((List)BookNoteList).size(); ++i) {
//                Map<String, Object> mapBookNote = (Map)((List)BookNoteList).get(i);
//                String mBookNoteDataItemTmp = mapBookNote.get("context").toString();
//                ParseNoteXml nParseNoteXml = new ParseNoteXml(this.mcontext);
//                nParseNoteXml.ParseNoteXmlData(mBookNoteDataItemTmp);
//                String strAttributeValue = nParseNoteXml.GetAttributeValue("NoteID");
//                if (strNotesId.equalsIgnoreCase(strAttributeValue)) {
//                    String strAddTime = nParseNoteXml.GetAttributeValue("Time");
//                    String strData;
//                    if (EditType == 0) {
//                        strData = nParseNoteXml.GetXmlData();
//                        int NotesLen = strData.indexOf("<Notes>");
//                        String StrPt = "";
//                        if (NotesLen != -1) {
//                            StrPt = strData.substring(0, NotesLen);
//                        } else {
//                            StrPt = strData.substring(0, strData.length());
//                        }
//
//                        String StrNotes = "";
//                        if (strParams.length() != 0) {
//                            StrNotes = "<Notes>" + strParams + "</Notes>";
//                        }
//
//                        strData = StrPt + StrNotes;
//                        nParseNoteXml.ClearxmlData();
//                        nParseNoteXml.SetXmlData(strData);
//                    } else if (EditType == 1) {
//                        nParseNoteXml.SetAttributeValue("Color", strParams);
//                    }
//
//                    strData = mapBookNote.get("serverId").toString();
//                    String strContent = nParseNoteXml.getxml();
//                    mBookNoteDao.updata(strBookDaoId, strContent);
//                    Map<String, Object> params = new HashMap();
//                    params.put("id", strData);
//                    params.put("bookId", strBookId);
//                    if (strPageNum.length() == 0) {
//                        strPageNum = "-1";
//                    }
//
//                    params.put("pageNum", strPageNum);
//                    params.put("annotationIndex", strNotesId);
//                    params.put("addTime", strAddTime);
//                    params.put("annotation", strContent);
//                    break;
//                }
//            }
//        }
//
//    }
}
