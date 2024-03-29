package com.artifex.mupdfdemo;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import com.artifex.mupdfdemo.MuPDFCore.Cookie;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.text.method.PasswordTransformationMethod;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.github.react.sextant.R;
import com.github.react.sextant.RCTMuPdfModule;

/* This enum should be kept in line with the cooresponding C enum in mupdf.c */
enum SignatureState {
	NoSupport,
	Unsigned,
	Signed
}

abstract class PassClickResultVisitor {
	public abstract void visitText(PassClickResultText result);
	public abstract void visitChoice(PassClickResultChoice result);
	public abstract void visitSignature(PassClickResultSignature result);
}

class PassClickResult {
	public final boolean changed;

	public PassClickResult(boolean _changed) {
		changed = _changed;
	}

	public void acceptVisitor(PassClickResultVisitor visitor) {
	}
}

class PassClickResultText extends PassClickResult {
	public final String text;

	public PassClickResultText(boolean _changed, String _text) {
		super(_changed);
		text = _text;
	}

	public void acceptVisitor(PassClickResultVisitor visitor) {
		visitor.visitText(this);
	}
}

class PassClickResultChoice extends PassClickResult {
	public final String [] options;
	public final String [] selected;

	public PassClickResultChoice(boolean _changed, String [] _options, String [] _selected) {
		super(_changed);
		options = _options;
		selected = _selected;
	}

	public void acceptVisitor(PassClickResultVisitor visitor) {
		visitor.visitChoice(this);
	}
}

class PassClickResultSignature extends PassClickResult {
	public final SignatureState state;

	public PassClickResultSignature(boolean _changed, int _state) {
		super(_changed);
		state = SignatureState.values()[_state];
	}

	public void acceptVisitor(PassClickResultVisitor visitor) {
		visitor.visitSignature(this);
	}
}

public class MuPDFPageView extends PageView implements MuPDFView {
	final private FilePicker.FilePickerSupport mFilePickerSupport;
	private final MuPDFCore mCore;
	private AsyncTask<Void,Void,PassClickResult> mPassClick;
	private RectF mWidgetAreas[];
	private Annotation mAnnotations[];
	private int mSelectedAnnotationIndex = -1;
	private int mSelectedFreetextIndex = -1;
	private AsyncTask<Void,Void,RectF[]> mLoadWidgetAreas;
	private AsyncTask<Void,Void,Annotation[]> mLoadAnnotations;
	private AlertDialog.Builder mTextEntryBuilder;
	private AlertDialog.Builder mChoiceEntryBuilder;
	private AlertDialog.Builder mSigningDialogBuilder;
	private AlertDialog.Builder mSignatureReportBuilder;
	private AlertDialog.Builder mPasswordEntryBuilder;
	private EditText mPasswordText;
	private AlertDialog mTextEntry;
	private AlertDialog mPasswordEntry;
	private EditText mEditText;
	private AsyncTask<String,Void,Boolean> mSetWidgetText;
	private AsyncTask<String,Void,Void> mSetWidgetChoice;
	private AsyncTask<PointF[],Void,Void> mAddStrikeOut;
	private AsyncTask<Object,Void,Void> mAddInk;
	private AsyncTask<Integer,Void,Void> mDeleteAnnotation;
	private AsyncTask<Void,Void,String> mCheckSignature;
	private AsyncTask<Void,Void,Boolean> mSign;
	private Runnable changeReporter;

	public MuPDFPageView(Context c, FilePicker.FilePickerSupport filePickerSupport, MuPDFCore core, Point parentSize, MuPDFPageAdapter adapter) {
		super(c, parentSize, adapter);
		mFilePickerSupport = filePickerSupport;
		mCore = core;
		mTextEntryBuilder = new AlertDialog.Builder(c);
		mTextEntryBuilder.setTitle(getContext().getString(R.string.fill_out_text_field));
		LayoutInflater inflater = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mEditText = (EditText)inflater.inflate(R.layout.textentry, null);
		mTextEntryBuilder.setView(mEditText);
		mTextEntryBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		mTextEntryBuilder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				mSetWidgetText = new AsyncTask<String,Void,Boolean> () {
					@Override
					protected Boolean doInBackground(String... arg0) {
						return mCore.setFocusedWidgetText(mPageNumber, arg0[0]);
					}
					@Override
					protected void onPostExecute(Boolean result) {
						changeReporter.run();
						if (!result)
							invokeTextDialog(mEditText.getText().toString());
					}
				};

				mSetWidgetText.execute(mEditText.getText().toString());
			}
		});
		mTextEntry = mTextEntryBuilder.create();

		mChoiceEntryBuilder = new AlertDialog.Builder(c);
		mChoiceEntryBuilder.setTitle(getContext().getString(R.string.choose_value));

		mSigningDialogBuilder = new AlertDialog.Builder(c);
		mSigningDialogBuilder.setTitle("Select certificate and sign?");
		mSigningDialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		mSigningDialogBuilder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				FilePicker picker = new FilePicker(mFilePickerSupport) {
					@Override
					void onPick(Uri uri) {
						signWithKeyFile(uri);
					}
				};

				picker.pick();
			}
		});

		mSignatureReportBuilder = new AlertDialog.Builder(c);
		mSignatureReportBuilder.setTitle("Signature checked");
		mSignatureReportBuilder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		mPasswordText = new EditText(c);
		mPasswordText.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
		mPasswordText.setTransformationMethod(new PasswordTransformationMethod());

		mPasswordEntryBuilder = new AlertDialog.Builder(c);
		mPasswordEntryBuilder.setTitle(R.string.enter_password);
		mPasswordEntryBuilder.setView(mPasswordText);
		mPasswordEntryBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		mPasswordEntry = mPasswordEntryBuilder.create();
	}

	private void signWithKeyFile(final Uri uri) {
		mPasswordEntry.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		mPasswordEntry.setButton(AlertDialog.BUTTON_POSITIVE, "Sign", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				signWithKeyFileAndPassword(uri, mPasswordText.getText().toString());
			}
		});

		mPasswordEntry.show();
	}

	private void signWithKeyFileAndPassword(final Uri uri, final String password) {
		mSign = new AsyncTask<Void,Void,Boolean>() {
			@Override
			protected Boolean doInBackground(Void... params) {
				return mCore.signFocusedSignature(Uri.decode(uri.getEncodedPath()), password);
			}
			@Override
			protected void onPostExecute(Boolean result) {
				if (result)
				{
					changeReporter.run();
				}
				else
				{
					mPasswordText.setText("");
					signWithKeyFile(uri);
				}
			}

		};

		mSign.execute();
	}

	public LinkInfo hitLink(float x, float y) {
		// Since link highlighting was implemented, the super class
		// PageView has had sufficient information to be able to
		// perform this method directly. Making that change would
		// make MuPDFCore.hitLinkPage superfluous.
		float scale = mSourceScale*(float)getWidth()/(float)mSize.x;
		float docRelX = (x - getLeft())/scale;
		float docRelY = (y - getTop())/scale;

		for (LinkInfo l: mLinks)
			if (l.rect.contains(docRelX, docRelY))
				return l;

		return null;
	}

	private void invokeTextDialog(String text) {
		mEditText.setText(text);
		mTextEntry.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		mTextEntry.show();
	}

	private void invokeChoiceDialog(final String [] options) {
		mChoiceEntryBuilder.setItems(options, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				mSetWidgetChoice = new AsyncTask<String,Void,Void>() {
					@Override
					protected Void doInBackground(String... params) {
						String [] sel = {params[0]};
						mCore.setFocusedWidgetChoiceSelected(sel);
						return null;
					}

					@Override
					protected void onPostExecute(Void result) {
						changeReporter.run();
					}
				};

				mSetWidgetChoice.execute(options[which]);
			}
		});
		AlertDialog dialog = mChoiceEntryBuilder.create();
		dialog.show();
	}

	private void invokeSignatureCheckingDialog() {
		mCheckSignature = new AsyncTask<Void,Void,String> () {
			@Override
			protected String doInBackground(Void... params) {
				return mCore.checkFocusedSignature();
			}
			@Override
			protected void onPostExecute(String result) {
				AlertDialog report = mSignatureReportBuilder.create();
				report.setMessage(result);
				report.show();
			}
		};

		mCheckSignature.execute();
	}

	private void invokeSigningDialog() {
		AlertDialog dialog = mSigningDialogBuilder.create();
		dialog.show();
	}

	private void warnNoSignatureSupport() {
//		AlertDialog dialog = mSignatureReportBuilder.create();
//		dialog.setTitle("App built with no signature support");
//		dialog.show();
	}

	public void setChangeReporter(Runnable reporter) {
		changeReporter = reporter;
	}

	public Hit passClickEvent(float x, float y) {
		float scale = mSourceScale*(float)getWidth()/(float)mSize.x;
		final float docRelX = (x - getLeft())/scale;
		final float docRelY = (y - getTop())/scale;
		boolean hit = false;
		int i;
		int t;

		//自定义文本批注
		mSelectedFreetextIndex = -1;
		for (t=0; t < mCloudData.getmFreetext().size(); t++) {
			HashMap map = mCloudData.getmFreetext().get(t);
			PointF p1 = new PointF((float)map.get("x"),(float)map.get("y"));
			PointF p2 = new PointF((float)map.get("x")+(float)map.get("width"),(float)map.get("y"));
			PointF p3 = new PointF((float)map.get("x")+(float)map.get("width"),(float)map.get("y")+(float)map.get("height"));
			PointF p4 = new PointF((float)map.get("x"),(float)map.get("y")+(float)map.get("height"));

			PointF p = new PointF(docRelX, docRelY);

			if (IsPointInMatrix(p1,p2,p3,p4,p)&&mPageNumber == (int)map.get("page")) {
				mSelectedFreetextIndex = t;
				setItemSelectBox(new Annotation((float)map.get("x"),(float)map.get("y"),(float)map.get("x")+(float)map.get("width"),(float)map.get("y")+(float)map.get("height"),Annotation.Type.FREETEXT.ordinal()));
				return Hit.FreeText;
			}
		}

		//原生批注
		if (mAnnotations != null) {
			for (i = 0; i < mAnnotations.length; i++)
				if (mAnnotations[i].contains(docRelX, docRelY)) {
					hit = true;
					break;
				}

			if (hit) {
				switch (mAnnotations[i].type) {
					case HIGHLIGHT:
					case UNDERLINE:
					case SQUIGGLY:
					case STRIKEOUT:
					case INK:
						mSelectedAnnotationIndex = i;
						setItemSelectBox(mAnnotations[i]);
						return Hit.Annotation;
				}
			}
		}

		mSelectedAnnotationIndex = -1;
		setItemSelectBox(null);

		if (!mCore.javascriptSupported())
			return Hit.Nothing;

		if (mWidgetAreas != null) {
			for (i = 0; i < mWidgetAreas.length && !hit; i++)
				if (mWidgetAreas[i].contains(docRelX, docRelY))
					hit = true;
		}

		if (hit) {
			mPassClick = new AsyncTask<Void,Void,PassClickResult>() {
				@Override
				protected PassClickResult doInBackground(Void... arg0) {
					return mCore.passClickEvent(mPageNumber, docRelX, docRelY);
				}

				@Override
				protected void onPostExecute(PassClickResult result) {
					if (result.changed) {
						changeReporter.run();
					}

					result.acceptVisitor(new PassClickResultVisitor() {
						@Override
						public void visitText(PassClickResultText result) {
							invokeTextDialog(result.text);
						}

						@Override
						public void visitChoice(PassClickResultChoice result) {
							invokeChoiceDialog(result.options);
						}

						@Override
						public void visitSignature(PassClickResultSignature result) {
							switch (result.state) {
								case NoSupport:
									warnNoSignatureSupport();
									break;
								case Unsigned:
									invokeSigningDialog();
									break;
								case Signed:
									invokeSignatureCheckingDialog();
									break;
							}
						}
					});
				}
			};

			mPassClick.execute();
			return Hit.Widget;
		}

		return Hit.Nothing;
	}

	public int getFreetextIndex(){
		return mSelectedFreetextIndex;
	}
	public int getSelectedAnnotationIndex(){
		return mSelectedAnnotationIndex;
	}

	/**
	 * 判断点是否在一个矩形内
	 * **/
	boolean IsPointInMatrix(PointF p1,PointF p2,PointF p3,PointF p4,PointF p){
		return GetCross(p1, p2, p) * GetCross(p3, p4, p) >= 0 && GetCross(p2, p3, p) * GetCross(p4, p1, p) >= 0;
	}
	/**
	 * IsPointInMatrix辅助函数
	 * **/
	float GetCross(PointF p1,PointF p2, PointF p){
		return (p2.x - p1.x) * (p.y - p1.y) - (p.x - p1.x) * (p2.y - p1.y);
	}

	@TargetApi(11)
	public boolean copySelection() {
		final StringBuilder text = getSelectedString();

		if (text.length() == 0)
			return false;

		int currentApiVersion = Build.VERSION.SDK_INT;
		if (currentApiVersion >= Build.VERSION_CODES.HONEYCOMB) {
			android.content.ClipboardManager cm = (android.content.ClipboardManager)mContext.getSystemService(Context.CLIPBOARD_SERVICE);

			cm.setPrimaryClip(ClipData.newPlainText("MuPDF", text));
		} else {
			android.text.ClipboardManager cm = (android.text.ClipboardManager)mContext.getSystemService(Context.CLIPBOARD_SERVICE);
			cm.setText(text);
		}

		deselectText();

		return true;
	}

	public StringBuilder getSelectedString() {
		final StringBuilder text = new StringBuilder();

		processSelectedText(new TextProcessor() {
			StringBuilder line;

			public void onStartLine() {
				line = new StringBuilder();
			}

			public void onWord(TextWord word) {
//				if (line.length() > 0)
//					line.append(' ');
				line.append(word.w);
			}

			public void onEndLine() {
				if (text.length() > 0)
					text.append('\n');
				text.append(line);
			}
		});

		return text;
	}

	/**
	 * 下划线添加确认
	 * **/
	public boolean markupSelection(final Annotation.Type type) {
		final ArrayList<PointF> quadPoints = new ArrayList<PointF>();
		processSelectedText(new TextProcessor() {
			RectF rect;

			public void onStartLine() {
				rect = new RectF();
			}

			public void onWord(TextWord word) {
				rect.union(word);
			}

			public void onEndLine() {
				if (!rect.isEmpty()) {
					quadPoints.add(new PointF(rect.left, rect.bottom));
					quadPoints.add(new PointF(rect.right, rect.bottom));
					quadPoints.add(new PointF(rect.right, rect.top));
					quadPoints.add(new PointF(rect.left, rect.top));
				}
			}
		});

		if (quadPoints.size() == 0)
			return false;

		mAddStrikeOut = new AsyncTask<PointF[],Void,Void>() {
			@Override
			protected Void doInBackground(PointF[]... params) {
				addMarkup(params[0], type);
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				loadAnnotations();
				update();
			}
		};

		mAddStrikeOut.execute(quadPoints.toArray(new PointF[quadPoints.size()]));

		deselectText();

		return true;
	}

	public boolean markupSelection(final int page, final PointF[] quadPoints, final Annotation.Type type) {

		mAddStrikeOut = new AsyncTask<PointF[],Void,Void>() {
			@Override
			protected Void doInBackground(PointF[]... params) {
				mCore.addMarkupAnnotation(page, quadPoints, type);
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				loadAnnotations();
				update();
			}
		};

		mAddStrikeOut.execute(quadPoints);

		deselectText();

		return true;
	}

	public void deleteSelectedAnnotation() {
		if(mSelectedFreetextIndex != -1){
			mCloudData.remove(mSelectedFreetextIndex);
			mSelectedFreetextIndex = -1;
			setItemSelectBox(null);

		}else if (mSelectedAnnotationIndex != -1) {
			if (mDeleteAnnotation != null)
				mDeleteAnnotation.cancel(true);

			mDeleteAnnotation = new AsyncTask<Integer,Void,Void>() {
				@Override
				protected Void doInBackground(Integer... params) {
					mCore.deleteAnnotation(mPageNumber, params[0]);
					deleteRemark(mPageNumber, params[0]);
					return null;
				}

				@Override
				protected void onPostExecute(Void result) {
					loadAnnotations();
					update();
				}
			};

			mDeleteAnnotation.execute(mSelectedAnnotationIndex);

			mSelectedAnnotationIndex = -1;
			setItemSelectBox(null);
		}
	}

	public void deleteSelectedAnnotation(final int page, final int annot_index){
		if (annot_index != -1) {
			if (mDeleteAnnotation != null)
				mDeleteAnnotation.cancel(true);
			mDeleteAnnotation = new AsyncTask<Integer,Void,Void>() {
				@Override
				protected Void doInBackground(Integer... params) {
					mCore.deleteAnnotation(page, params[0]);
					deleteRemark(page, params[0]);
					return null;
				}

				@Override
				protected void onPostExecute(Void result) {
					loadAnnotations();
					update();
				}
			};

			mDeleteAnnotation.execute(annot_index);
		}
	}

	/**
	 * 添加备注
	 * **/
	public void addRemark(int index){
		if(index == -1) {
			final ArrayList<PointF> quadPoints = new ArrayList<PointF>();
			processSelectedText(new TextProcessor() {
				RectF rect;

				public void onStartLine() {
					rect = new RectF();
				}

				public void onWord(TextWord word) {
					rect.union(word);
				}

				public void onEndLine() {
					if (!rect.isEmpty()) {
						quadPoints.add(new PointF(rect.left, rect.bottom));
						quadPoints.add(new PointF(rect.right, rect.bottom));
						quadPoints.add(new PointF(rect.right, rect.top));
						quadPoints.add(new PointF(rect.left, rect.top));
					}
				}
			});

			if (quadPoints.size() > 0) {
				mAddStrikeOut = new AsyncTask<PointF[], Void, Void>() {
					@Override
					protected Void doInBackground(PointF[]... params) {
						addMarkup(params[0], Annotation.Type.UNDERLINE);
						return null;
					}

					@Override
					protected void onPostExecute(Void result) {
						loadAnnotations(quadPoints);
						update();
					}
				};

				mAddStrikeOut.execute(quadPoints.toArray(new PointF[quadPoints.size()]));

				deselectText();
			}
		}else if(mItemSelectBox != null){
			addRemark(mItemSelectBox.right,mItemSelectBox.top,index);
			deselectAnnotation();
		}
	}

	private void addRemark(float x, float y, int index){
		boolean execute = true;

		RemarkItem item = new RemarkItem();
		item.x = x;
		item.y = y;
		item.page = mPageNumber;
		item.index = index;

		for(int i=0;i<mCloudData.getmRemarkList().size();i++) {
			if(mCloudData.getmRemarkList().get(i).page == mPageNumber && mCloudData.getmRemarkList().get(i).index == index){
				mCloudData.getmRemarkList().get(i).x = x;
				mCloudData.getmRemarkList().get(i).y = y;
				execute = false;
				break;
			}
		}

		if(execute){
			mCloudData.add(item);
		}
		mCustomerView.invalidate();
	}

	public String getScreenShot(){
		if(mEntireBm!=null && mSourceScale > 0){
			RectF rect = new RectF(
					mItemSelectBox!=null?mItemSelectBox.left*mSourceScale:mSelectBox!=null?mSelectBox.left*mSourceScale-20:0,
					mItemSelectBox!=null?mItemSelectBox.top*mSourceScale:mSelectBox!=null?mSelectBox.top*mSourceScale-20:0,
					mItemSelectBox!=null?mItemSelectBox.right*mSourceScale:mSelectBox!=null?mSelectBox.right*mSourceScale+20:0,
					mItemSelectBox!=null?mItemSelectBox.bottom*mSourceScale:mSelectBox!=null?mSelectBox.bottom*mSourceScale+20:0
			);
			return bitmapToString(Bitmap.createBitmap(mEntireBm,(int)rect.left,(int)rect.top,(int)Math.abs(rect.right-rect.left),(int)Math.abs(rect.top-rect.bottom)));
		}
		return "";
	}

	public Bitmap getBitmap(){
		return mEntireBm;
	}

	String bitmapToString(Bitmap bitmap) {
		if(bitmap == null) return "";
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
		byte[] imgBytes = baos.toByteArray();
		return Base64.encodeToString(imgBytes, Base64.DEFAULT);
	}

	/**
	 * 删除备注
	 * **/
	private void deleteRemark(int page, int annotationIndex){
		for(int i = 0; i < mCloudData.getmRemarkList().size(); i++) {
			if(mCloudData.getmRemarkList().get(i).page == page &&
			   mCloudData.getmRemarkList().get(i).index == annotationIndex
			){
				mCloudData.getmRemarkList().remove(i);
				break;
			}
		}

		for(int j = 0; j < mCloudData.getmRemarkList().size(); j++) {
			if(mCloudData.getmRemarkList().get(j).page == page &&
			   mCloudData.getmRemarkList().get(j).index > annotationIndex
			){
				mCloudData.getmRemarkList().get(j).index -= 1;
			}
		}
		mCustomerView.invalidate();

	}

	public void deselectAnnotation() {
		mSelectedAnnotationIndex = -1;
		setItemSelectBox(null);
	}

	@Override
	public void setDraw(PointF[][] arcs) {
		super.setDraw(arcs);
	}

	public boolean saveDraw() {
		PointF[][] path = getDraw();

		if (path == null)
			return false;

		if (mAddInk != null) {
			mAddInk.cancel(true);
			mAddInk = null;
		}
		mAddInk = new AsyncTask<Object,Void,Void>() {
			@Override
			protected Void doInBackground(Object... params) {
				mCore.addInkAnnotation(mPageNumber, (PointF[][])params[0], (float[])params[1], (float)params[2]);
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				loadAnnotations();
				update();
			}

		};

		mAddInk.execute(getDraw(), getColor(), getInkThickness());
		cancelDraw();

		return true;
	}

	public boolean saveDraw(final int page, PointF[][] arcs) {

		if (arcs == null)
			return false;

		if (mAddInk != null) {
			mAddInk.cancel(true);
			mAddInk = null;
		}

		mAddInk = new AsyncTask<Object,Void,Void>() {
			@Override
			protected Void doInBackground(Object... params) {
				mCore.addInkAnnotation(page, (PointF[][])params[0], (float[])params[1], (float)params[2]);
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				loadAnnotations();
				update();
			}

		};

		mAddInk.execute(arcs, getColor(), getInkThickness());

		return true;
	}


	@Override
	protected CancellableTaskDefinition<Void, Void> getDrawPageTask(final Bitmap bm, final int sizeX, final int sizeY,
			final int patchX, final int patchY, final int patchWidth, final int patchHeight) {
		return new MuPDFCancellableTaskDefinition<Void, Void>(mCore) {
			@Override
			public Void doInBackground(Cookie cookie, Void ... params) {
				// Workaround bug in Android Honeycomb 3.x, where the bitmap generation count
				// is not incremented when drawing.
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB &&
						Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
					bm.eraseColor(0);
				mCore.drawPage(bm, mPageNumber, sizeX, sizeY, patchX, patchY, patchWidth, patchHeight, cookie);
				return null;
			}
		};

	}

	protected CancellableTaskDefinition<Void, Void> getUpdatePageTask(final Bitmap bm, final int sizeX, final int sizeY,
			final int patchX, final int patchY, final int patchWidth, final int patchHeight)
	{
		return new MuPDFCancellableTaskDefinition<Void, Void>(mCore) {

			@Override
			public Void doInBackground(Cookie cookie, Void ... params) {
				// Workaround bug in Android Honeycomb 3.x, where the bitmap generation count
				// is not incremented when drawing.
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB &&
						Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
					bm.eraseColor(0);
				mCore.updatePage(bm, mPageNumber, sizeX, sizeY, patchX, patchY, patchWidth, patchHeight, cookie);
				return null;
			}
		};
	}

	@Override
	protected LinkInfo[] getLinkInfo() {
		return mCore.getPageLinks(mPageNumber);
	}

	@Override
	protected TextWord[][] getText() {
		return mCore.textLines(mPageNumber);
	}

	@Override
	protected void addMarkup(PointF[] quadPoints, Annotation.Type type) {
		mCore.addMarkupAnnotation(mPageNumber, quadPoints, type);
	}

	private void loadAnnotations() {
		mAnnotations = null;
		if (mLoadAnnotations != null)
			mLoadAnnotations.cancel(true);
		mLoadAnnotations = new AsyncTask<Void,Void,Annotation[]> () {
			@Override
			protected Annotation[] doInBackground(Void... params) {
				return mCore.getAnnoations(mPageNumber);
			}

			@Override
			protected void onPostExecute(Annotation[] result) {
				mAnnotations = result;
			}
		};

		mLoadAnnotations.execute();
	}

	/**添加批注同时添加备注**/
	private void loadAnnotations(final ArrayList<PointF> quadPoints) {
		mAnnotations = null;
		if (mLoadAnnotations != null)
			mLoadAnnotations.cancel(true);
		mLoadAnnotations = new AsyncTask<Void,Void,Annotation[]> () {
			@Override
			protected Annotation[] doInBackground(Void... params) {
				return mCore.getAnnoations(mPageNumber);
			}

			@Override
			protected void onPostExecute(Annotation[] result) {
				mAnnotations = result;
				addRemark(quadPoints.get(2).x,quadPoints.get(2).y,result.length-1);
			}
		};

		mLoadAnnotations.execute();
	}

	@Override
	public Annotation.Type getAnnotationType() {
		return mAnnotationType;
	}

	@Override
	public void setPage(final int page, PointF size) {
		mLoadWidgetAreas = new AsyncTask<Void,Void,RectF[]> () {
			@Override
			protected RectF[] doInBackground(Void... arg0) {
				return mCore.getWidgetAreas(page);
			}

			@Override
			protected void onPostExecute(RectF[] result) {
				loadAnnotations();
				mWidgetAreas = result;
			}
		};

		mLoadWidgetAreas.execute();

		super.setPage(page, size);
	}

	public void setScale(float scale) {
		// This type of view scales automatically to fit the size
		// determined by the parent view groups during layout
	}

	@Override
	public void releaseResources() {
		if (mPassClick != null) {
			mPassClick.cancel(true);
			mPassClick = null;
		}

		if (mLoadWidgetAreas != null) {
			mLoadWidgetAreas.cancel(true);
			mLoadWidgetAreas = null;
		}

		if (mLoadAnnotations != null) {
			mLoadAnnotations.cancel(true);
			mLoadAnnotations = null;
		}

		if (mSetWidgetText != null) {
			mSetWidgetText.cancel(true);
			mSetWidgetText = null;
		}

		if (mSetWidgetChoice != null) {
			mSetWidgetChoice.cancel(true);
			mSetWidgetChoice = null;
		}

		if (mAddStrikeOut != null) {
			mAddStrikeOut.cancel(true);
			mAddStrikeOut = null;
		}

		if (mDeleteAnnotation != null) {
			mDeleteAnnotation.cancel(true);
			mDeleteAnnotation = null;
		}

		super.releaseResources();
	}

	@Override
	public float getScale() {
		return 0;
	}
}
