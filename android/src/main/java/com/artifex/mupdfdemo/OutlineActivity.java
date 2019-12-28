package com.artifex.mupdfdemo;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.github.react.sextant.R;

public class OutlineActivity extends ListActivity {
	OutlineItem mItems[];
	String mFileName;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mItems = OutlineActivityData.get().items;
		setListAdapter(new OutlineAdapter(getLayoutInflater(),mItems));
		// Restore the position within the list from last viewing
		getListView().setSelection(OutlineActivityData.get().position);
		getListView().setDividerHeight(0);
		setResult(-1);

		View header = getLayoutInflater().inflate(R.layout.mupdf_topbar, getListView(), false);
		header.findViewById(R.id.search_button).setVisibility(View.INVISIBLE);
		Intent intent = getIntent();
		mFileName = intent.getStringExtra("fileName");
		TextView idFileName = (TextView)header.findViewById(R.id.idFileName);
		idFileName.setText(mFileName);
		getListView().addHeaderView(header);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		OutlineActivityData.get().position = getListView().getFirstVisiblePosition();
		setResult(mItems[position-1].page);
		finish();
	}

	public void OnOpenSearchButtonClick(View v){

	}

	public void onFinishActivity(View v){
		finish();
	}

	public void onBubbling(View v){

	}
}
