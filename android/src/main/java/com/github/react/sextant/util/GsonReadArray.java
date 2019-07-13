package com.github.react.sextant.util;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

class User {

    private final String annotationIndex;
    private final String bookId;
    private final String context;
    private final String id;
    private final String pageNum;
    private final String serverId;

    public User(
            String annotationIndex,
            String bookId,
            String context,
            String id,
            String pageNum,
            String serverId
    ) {
        this.annotationIndex = annotationIndex;
        this.bookId = bookId;
        this.context = context;
        this.id = id;
        this.pageNum = pageNum;
        this.serverId = serverId;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{User").append("First name: ")
                .append(annotationIndex).append(", Last name: ")
                .append(bookId).append("}").toString();
    }
}

public class GsonReadArray {

    public void stringToArray(String args) {

        Gson gson = new GsonBuilder().create();

        User[] users = gson.fromJson(args, User[].class);

        int length =users.length;
        for(int i=0;i<length;i++){
            Log.i("LUOKUN"+i,users[i].toString());
        }


//            Arrays.stream(users).forEach(e -> {
//                System.out.println(e);
//            });
    }
}
