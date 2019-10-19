package com.artifex.mupdfdemo;

import com.artifex.mupdfdemo.ReaderView.ViewMapper;
import com.artifex.utils.DigitalizedEventCallback;
import com.artifex.utils.SharedPreferencesUtil;
import com.artifex.utils.ThreadPerTaskExecutor;
import com.facebook.react.ReactActivity;
import com.github.react.sextant.MyListener;
import com.github.react.sextant.R;
import com.github.react.sextant.RCTMuPdfModule;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ViewAnimator;

public class MuPDFActivity extends ReactActivity implements FilePicker.FilePickerSupport
{
    private LinearLayout mSearchBar;
    private ViewAnimator mBottomBarSwitcher;
    private ViewAnimator mAcceptSwitcher;
    private Vibrator vibrator;
    private RelativeLayout bookselecttextup;
    private RelativeLayout bookselecttextdown;
    private RelativeLayout bookselectmenu;
    private RelativeLayout annotationselectmenu;

    /* The core rendering instance */
    enum TopBarMode {Main, Search, Accept};

    private final int    OUTLINE_REQUEST=0;
    private final int    PRINT_REQUEST=1;
    private final int    FILEPICK_REQUEST=2;
    private MuPDFCore    core;
    private String       mFileName;
    private String       mFilePath;
    private int          mPage;
    private MuPDFReaderView mDocView;
    private View         mButtonsView;
    private boolean      mButtonsVisible;
    private EditText     mPasswordView;
    private TextView     mFilenameView;
    private SeekBar      mPageSlider;
    private int          mPageSliderRes;
    private TextView     mPageNumberView;
    private TextView     mInfoView;
    private ViewAnimator mTopBarSwitcher;
    private TopBarMode   mTopBarMode = TopBarMode.Main;
    private ImageButton  mSearchBack;
    private ImageButton  mSearchFwd;
    private EditText     mSearchText;
    private SearchTask   mSearchTask;
    private AlertDialog.Builder mAlertBuilder;
    private boolean    mLinkHighlight = false;
    private final Handler mHandler = new Handler();
    private boolean mAlertsActive= false;
    private boolean mReflow = false;
    private AsyncTask<Void,Void,MuPDFAlert> mAlertTask;
    private AlertDialog mAlertDialog;
    private FilePicker mFilePicker;

    public void createAlertWaiter() {
        mAlertsActive = true;
        if (mAlertTask != null) {
            mAlertTask.cancel(true);
            mAlertTask = null;
        }
        if (mAlertDialog != null) {
            mAlertDialog.cancel();
            mAlertDialog = null;
        }
        mAlertTask = new AsyncTask<Void,Void,MuPDFAlert>() {

            @Override
            protected MuPDFAlert doInBackground(Void... arg0) {
                if (!mAlertsActive)
                    return null;

                return core.waitForAlert();
            }

            @Override
            protected void onPostExecute(final MuPDFAlert result) {
                if (result == null)
                    return;
                final MuPDFAlert.ButtonPressed pressed[] = new MuPDFAlert.ButtonPressed[3];
                for(int i = 0; i < 3; i++)
                    pressed[i] = MuPDFAlert.ButtonPressed.None;
                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mAlertDialog = null;
                        if (mAlertsActive) {
                            int index = 0;
                            switch (which) {
                                case AlertDialog.BUTTON1: index=0; break;
                                case AlertDialog.BUTTON2: index=1; break;
                                case AlertDialog.BUTTON3: index=2; break;
                            }
                            result.buttonPressed = pressed[index];
                            core.replyToAlert(result);
                            createAlertWaiter();
                        }
                    }
                };
                mAlertDialog = mAlertBuilder.create();
                mAlertDialog.setTitle(result.title);
                mAlertDialog.setMessage(result.message);
                switch (result.iconType)
                {
                    case Error:
                        break;
                    case Warning:
                        break;
                    case Question:
                        break;
                    case Status:
                        break;
                }
                switch (result.buttonGroupType)
                {
                    case OkCancel:
                        mAlertDialog.setButton(AlertDialog.BUTTON2, getString(R.string.cancel), listener);
                        pressed[1] = MuPDFAlert.ButtonPressed.Cancel;
                    case Ok:
                        mAlertDialog.setButton(AlertDialog.BUTTON1, getString(R.string.okay), listener);
                        pressed[0] = MuPDFAlert.ButtonPressed.Ok;
                        break;
                    case YesNoCancel:
                        mAlertDialog.setButton(AlertDialog.BUTTON3, getString(R.string.cancel), listener);
                        pressed[2] = MuPDFAlert.ButtonPressed.Cancel;
                    case YesNo:
                        mAlertDialog.setButton(AlertDialog.BUTTON1, getString(R.string.yes), listener);
                        pressed[0] = MuPDFAlert.ButtonPressed.Yes;
                        mAlertDialog.setButton(AlertDialog.BUTTON2, getString(R.string.no), listener);
                        pressed[1] = MuPDFAlert.ButtonPressed.No;
                        break;
                }
                mAlertDialog.setOnCancelListener(new OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        mAlertDialog = null;
                        if (mAlertsActive) {
                            result.buttonPressed = MuPDFAlert.ButtonPressed.None;
                            core.replyToAlert(result);
                            createAlertWaiter();
                        }
                    }
                });

                mAlertDialog.show();
            }
        };

        mAlertTask.executeOnExecutor(new ThreadPerTaskExecutor());
    }

    public void destroyAlertWaiter() {
        mAlertsActive = false;
        if (mAlertDialog != null) {
            mAlertDialog.cancel();
            mAlertDialog = null;
        }
        if (mAlertTask != null) {
            mAlertTask.cancel(true);
            mAlertTask = null;
        }
    }

    private MuPDFCore openFile(String path)
    {
        try
        {
            core = new MuPDFCore(this, path);
            OutlineActivityData.set(null);
        }
        catch (Exception e)
        {
            System.out.println(e);
            return null;
        }
        return core;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mAlertBuilder = new AlertDialog.Builder(this);

        if (core == null) {
            core = (MuPDFCore)getLastNonConfigurationInstance();

            if (savedInstanceState != null && savedInstanceState.containsKey("FileName")) {
                mFileName = savedInstanceState.getString("FileName");
            }
        }

        /**
         * Intent extra
         * **/
        if (core == null) {
            Intent intent = getIntent();
            mFileName = intent.getStringExtra("fileName");
            mFilePath = intent.getStringExtra("filePath");
            mPage = intent.getIntExtra("page",0);

            core = openFile(mFilePath);
            SearchTaskResult.set(null);

            if (core != null && core.needsPassword()) {
                requestPassword(savedInstanceState);
                return;
            }
            if (core != null && core.countPages() == 0)
            {
                core = null;
            }
        }
        if (core == null)
        {
            AlertDialog alert = mAlertBuilder.create();
            alert.setTitle(R.string.cannot_open_document);
            alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dismiss),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            RCTMuPdfModule.error = true;
                            finish();
                        }
                    });
            alert.setOnCancelListener(new OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {
                    RCTMuPdfModule.error = true;
                    finish();
                }
            });
            alert.show();
            return;
        }

        createUI(savedInstanceState);
    }


    /**
     * PDF需要输入密码的情况下显示
     * **/
    public void requestPassword(final Bundle savedInstanceState) {
        mPasswordView = new EditText(this);
        mPasswordView.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
        mPasswordView.setTransformationMethod(new PasswordTransformationMethod());

        AlertDialog alert = mAlertBuilder.create();
        alert.setTitle(R.string.enter_password);
        alert.setView(mPasswordView);
        alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.okay),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (core.authenticatePassword(mPasswordView.getText().toString())) {
                            createUI(savedInstanceState);
                        } else {
                            requestPassword(savedInstanceState);
                        }
                    }
                });
        alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        RCTMuPdfModule.error = true;
                        finish();
                    }
                });
        alert.show();
    }

    public void createUI(Bundle savedInstanceState) {
        if (core == null)
            return;

        // Now create the UI.
        // First create the document view
        mDocView = new MuPDFReaderView(this) {
            @Override
            protected void onMoveToChild(int i) {
                if (core == null)
                    return;
                mPageNumberView.setText(String.format("%d / %d", i + 1,
                        core.countPages()));
                mPageSlider.setMax((core.countPages() - 1) * mPageSliderRes);
                mPageSlider.setProgress(i * mPageSliderRes);

                SharedPreferencesUtil.CURRENT_PAGE = i;

                super.onMoveToChild(i);
            }

            @Override
            protected void onTapMainDocArea() {
                if (!mButtonsVisible) {
                    if(mTopBarMode == TopBarMode.Main)
                        showButtons();

                } else {
                    if (mTopBarMode == TopBarMode.Main)
                        hideButtons();
                }

            }

            @Override
            protected void onDocMotion() {
                hideButtons();

                if(annotationselectmenu.getVisibility() == VISIBLE){
                    annotationselectmenu.setVisibility(View.INVISIBLE);
                }
            }

            /**
             * MuPDFReaderView.Mode == Viewing
             * **/
            @Override
            protected void onHit(Hit item) {

//                switch (mTopBarMode) {
//                    case Annot:
//                        if (item == Hit.Annotation) {
//                            showButtons();
//                            mTopBarMode = TopBarMode.Delete;
//
//                        }
//                        break;
//                    case Delete:
//                        mTopBarMode = TopBarMode.Annot;
//
//                        // fall through
//                    default:
//                        // Not in annotation editing mode, but the pageview will
//                        // still select and highlight hit annotations, so
//                        // deselect just in case.
//                        MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
//                        if (pageView != null)
//                            pageView.deselectAnnotation();
//                        break;
//                }
            }
        };
        mDocView.setAdapter(new MuPDFPageAdapter(this, this, core));

        /**
         * @ReactMethod 监听MyListener
         * **/
        RCTMuPdfModule.setUpListener(new MyListener() {
            @Override
            public void onEvent(String str) {
                try{
                    MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
                    JsonParser jsonParser = new JsonParser();
                    JsonObject jsonObject = (JsonObject) jsonParser.parse(str);

                    switch (jsonObject.get("type").getAsString()){

                        /**
                         * 更新页面
                         *
                         * @key type: "update_page"
                         * @key page: int
                         * **/
                        case "update_page":
                            if(pageView!=null && jsonObject.get("page").getAsInt() >= 0 && jsonObject.get("page").getAsInt() != pageView.getPage()){
                                final int page = jsonObject.get("page").getAsInt();

                                runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {

                                        mDocView.setDisplayedViewIndex(page);

                                    }
                                });
                            }
                            break;
                        /**
                         * 更新批注
                         *
                         * @key type: "add_annotation"
                         * @key path: PointF[][]
                         * @key page: int
                         * **/
                        case "add_annotation":
                            JsonArray jsonArray = jsonObject.get("path").getAsJsonArray();
                            PointF[][] p=new PointF[jsonArray.size()][];
                            for(int i=0;i<jsonArray.size();i++){
                                JsonArray two = jsonArray.get(i).getAsJsonArray();
                                PointF [] points=new PointF[two.size()];
                                for(int j=0;j<two.size();j++){
                                    points[j] = new PointF(two.get(j).getAsJsonArray().get(0).getAsFloat(),two.get(j).getAsJsonArray().get(1).getAsFloat());
                                }
                                p[i] = points;
                            }

                            if (pageView != null){
                                pageView.saveDraw(jsonObject.get("page").getAsInt(),p);
                            }
                            break;
                        /**
                         * 更新标注
                         *
                         * @key type: "add_markup_annotation"
                         * @key path: PointF[]
                         * @key page: int
                         * @key annotation_type (enum)String
                         * **/
                        case "add_markup_annotation":
                            JsonArray jsonArray2 = jsonObject.get("path").getAsJsonArray();
                            PointF[] p2=new PointF[jsonArray2.size()];
                            for(int i=0;i<jsonArray2.size();i++){
                                p2[i] = new PointF(jsonArray2.get(i).getAsJsonArray().get(0).getAsFloat(),jsonArray2.get(i).getAsJsonArray().get(1).getAsFloat());
                            }
                            if (pageView != null){
                                switch (jsonObject.get("annotation_type").getAsString()){
                                    case "UNDERLINE":
                                        pageView.markupSelection(jsonObject.get("page").getAsInt(), p2, Annotation.Type.UNDERLINE);
                                        break;
                                    case "HIGHLIGHT":
                                        pageView.markupSelection(jsonObject.get("page").getAsInt(), p2, Annotation.Type.HIGHLIGHT);
                                        break;
                                }


                            }
                            break;
                        /**
                         * 删除批注
                         *
                         * @key type: "delete_annotation"
                         * @key annot_index: int
                         * @key page int
                         * **/
                        case "delete_annotation":
                            if (pageView != null){
                                pageView.deleteSelectedAnnotation(jsonObject.get("page").getAsInt(),jsonObject.get("annot_index").getAsInt());
                            }
                            break;
                    }

                }catch (Exception e) {

                }

            }
        });


        /**
         * 震动实例
         * **/
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        /**
         * EventCallback
         * **/
        mDocView.setEventCallback(new DigitalizedEventCallback(){
            @Override
            public void longPressOnPdfPosition(int page, float viewX, float viewY, float pdfX, float pdfY){
                if(mTopBarMode == TopBarMode.Main){
                    MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
                    if (pageView != null){
                        vibrator.vibrate(100);
                        mDocView.setMode(MuPDFReaderView.Mode.Selecting);
                        pageView.selectText(viewX, viewY, viewX, viewY);
                    }else {
                        hidePopMenu();
                    }
                }
            }

            @Override
            public void touchMoveOnPdfPosition(RectF rect, float scale){
                MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
                if(rect != null) {
                    showPopMenu();

                    float docRelX = rect.left * scale + pageView.getLeft()-bookselecttextup.getWidth()/2;
                    float docRelY = rect.top * scale + pageView.getTop();
                    float docRelRight = rect.right * scale + pageView.getLeft()-bookselecttextdown.getWidth()/2;
                    float docRelBottom = rect.bottom * scale + pageView.getTop();

                    bookselecttextup.setX(docRelX);
                    bookselecttextup.setY(docRelY-bookselecttextup.getMeasuredHeight());

                    if(rect.bottom>0){
                        bookselecttextdown.setX(docRelRight);
                        bookselecttextdown.setY(docRelBottom);
                    }

                    if(docRelX<0){
                        bookselectmenu.setX(0);
                    }else if(mDocView.getWidth() - docRelX <  bookselectmenu.getMeasuredWidth()){
                        bookselectmenu.setX(mDocView.getWidth()-bookselectmenu.getMeasuredWidth());
                    }else {
                        bookselectmenu.setX(docRelX);
                    }

                    if(docRelY<(bookselectmenu.getMeasuredHeight()+bookselecttextdown.getMeasuredHeight())){
                        bookselectmenu.setY((rect.bottom-rect.top) * scale+docRelY+bookselecttextdown.getMeasuredHeight());
                    }else {
                        bookselectmenu.setY(docRelY-bookselectmenu.getMeasuredHeight()-bookselecttextdown.getMeasuredHeight());
                    }
                }
            }

            @Override
            public void doubleTapOnPdfPosition(int page, float viewX, float viewY, float pdfX, float pdfY){
            }

            @Override
            public void singleTapOnPdfPosition(int page, float viewX, float viewY, float pdfX, float pdfY){
                if(bookselectmenu.getVisibility() == View.VISIBLE){
                    MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
                    if (pageView != null) {
                        pageView.deselectText();
                    }

                    hidePopMenu();
                }

            }

            @Override
            public void singleTapOnHit(RectF rect, float scale){
                MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
                if(rect != null) {
                    float docRelX = rect.left * scale + pageView.getLeft();
                    float docRelY = rect.top * scale + pageView.getTop();

                    annotationselectmenu.setVisibility(View.VISIBLE);

                    if(docRelX<0){
                        annotationselectmenu.setX(0);
                    }else if(mDocView.getWidth() - docRelX <  annotationselectmenu.getMeasuredWidth()){
                        annotationselectmenu.setX(mDocView.getWidth()-annotationselectmenu.getMeasuredWidth());
                    }else {
                        annotationselectmenu.setX(docRelX);
                    }

                    if(docRelY<annotationselectmenu.getMeasuredHeight()){
                        annotationselectmenu.setY((rect.bottom-rect.top) * scale+docRelY);
                    }else {
                        annotationselectmenu.setY(docRelY-annotationselectmenu.getMeasuredHeight());
                    }
                }else {
                    annotationselectmenu.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void error(String message){

            }
        });

        mSearchTask = new SearchTask(this, core) {
            @Override
            protected void onTextFound(SearchTaskResult result) {
                SearchTaskResult.set(result);
                // Ask the ReaderView to move to the resulting page
                mDocView.setDisplayedViewIndex(result.pageNumber);
                // Make the ReaderView act on the change to SearchTaskResult
                // via overridden onChildSetup method.
                mDocView.resetupChildren();
            }
        };

        // Make the buttons overlay, and store all its
        // controls in variables
        /**
         * DOM集合
         * **/
        makeButtonsView();

        // Set up the page slider
        int smax = Math.max(core.countPages()-1,1);
        mPageSliderRes = ((10 + smax - 1)/smax) * 2;

        // Set the file-name text
        mFilenameView.setText(mFileName);

        // Activate the seekbar
        mPageSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar seekBar) {
                mDocView.setDisplayedViewIndex((seekBar.getProgress()+mPageSliderRes/2)/mPageSliderRes);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {}

            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                updatePageNumView((progress+mPageSliderRes/2)/mPageSliderRes);
            }
        });

        // Activate the search-preparing button
//        mSearchButton.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                searchModeOn();
//            }
//        });

        // Activate the reflow button
//        mReflowButton.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                toggleReflow();
//            }
//        });

        if (core.fileFormat().startsWith("PDF") && core.isUnencryptedPDF() && !core.wasOpenedFromBuffer())
        {
//            mAnnotButton.setOnClickListener(new View.OnClickListener() {
//                public void onClick(View v) {
//                    mTopBarMode = TopBarMode.Annot;
//
//                }
//            });
        }
        else
        {
//            mAnnotButton.setVisibility(View.GONE);
        }

        mSearchBack.setEnabled(false);
        mSearchFwd.setEnabled(false);
        mSearchBack.setColorFilter(Color.argb(255, 128, 128, 128));
        mSearchFwd.setColorFilter(Color.argb(255, 128, 128, 128));

        mSearchText.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                boolean haveText = s.toString().length() > 0;
                setButtonEnabled(mSearchBack, haveText);
                setButtonEnabled(mSearchFwd, haveText);

                if (SearchTaskResult.get() != null && !mSearchText.getText().toString().equals(SearchTaskResult.get().txt)) {
                    SearchTaskResult.set(null);
                    mDocView.resetupChildren();
                }
            }
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {}
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {}
        });

        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE)
                    search(1);
                return false;
            }
        });

        mSearchText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER)
                    search(1);
                return false;
            }
        });

        mSearchBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                search(-1);
            }
        });
        mSearchFwd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                search(1);
            }
        });

        if (core.hasOutline()) {
//            mOutlineButton.setOnClickListener(new View.OnClickListener() {
//                public void onClick(View v) {
//                    OutlineItem outline[] = core.getOutline();
//                    if (outline != null) {
//                        OutlineActivityData.get().items = outline;
//                        Intent intent = new Intent(MuPDFActivity.this, OutlineActivity.class);
//                        startActivityForResult(intent, OUTLINE_REQUEST);
//                    }
//                }
//            });
        } else {
//            mOutlineButton.setVisibility(View.GONE);
        }

        // Reenstate last state if it was recorded
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        mDocView.setDisplayedViewIndex(prefs.getInt("page"+mFileName, 0));

        /**
         * 首次进入时的操作
         * **/
        if (savedInstanceState == null || !savedInstanceState.getBoolean("ButtonsHidden", false))
            mButtonsVisible = true;
            hideButtons();

        /**
         * 处理横竖屏时数据丢失问题
         * **/
        if(savedInstanceState != null && savedInstanceState.getBoolean("SearchMode", false))
            searchModeOn();

        if(savedInstanceState != null && savedInstanceState.getBoolean("ReflowMode", false))
            reflowModeSet(true);

        // Stick the document view and the buttons overlay into a parent view
        RelativeLayout layout = new RelativeLayout(this);
        layout.addView(mDocView);
        layout.addView(mButtonsView);
        setContentView(layout);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case OUTLINE_REQUEST:
                if (resultCode >= 0)
                    mDocView.setDisplayedViewIndex(resultCode);
                break;
            case PRINT_REQUEST:
                if (resultCode == RESULT_CANCELED)
                    showInfo(getString(R.string.print_failed));
                break;
            case FILEPICK_REQUEST:
                if (mFilePicker != null && resultCode == RESULT_OK)
                    mFilePicker.onPick(data.getData());
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public Object onRetainNonConfigurationInstance()
    {
        MuPDFCore mycore = core;
        core = null;
        return mycore;
    }

    private void reflowModeSet(boolean reflow)
    {
//        mReflow = reflow;
//        mDocView.setAdapter(mReflow ? new MuPDFReflowAdapter(this, core) : new MuPDFPageAdapter(this, this, core));
//        mReflowButton.setColorFilter(mReflow ? Color.argb(0xFF, 172, 114, 37) : Color.argb(0xFF, 255, 255, 255));
//        setButtonEnabled(mAnnotButton, !reflow);
//        setButtonEnabled(mSearchButton, !reflow);
//        if (reflow) setLinkHighlight(false);
//        setButtonEnabled(mLinkButton, !reflow);
//        setButtonEnabled(mMoreButton, !reflow);
//        mDocView.refresh(mReflow);
    }

    private void toggleReflow() {
        reflowModeSet(!mReflow);
        showInfo(mReflow ? getString(R.string.entering_reflow_mode) : getString(R.string.leaving_reflow_mode));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mFileName != null && mDocView != null) {
            outState.putString("FileName", mFileName);

            // Store current page in the prefs against the file name,
            // so that we can pick it up each time the file is loaded
            // Other info is needed only for screen-orientation change,
            // so it can go in the bundle
            SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putInt("page"+mFileName, mDocView.getDisplayedViewIndex());
            edit.commit();
        }

        if (!mButtonsVisible)
            outState.putBoolean("ButtonsHidden", true);

        if (mTopBarMode == TopBarMode.Search)
            outState.putBoolean("SearchMode", true);

        if (mReflow)
            outState.putBoolean("ReflowMode", true);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mSearchTask != null)
            mSearchTask.stop();

        if (mFileName != null && mDocView != null) {
            SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putInt("page"+mFileName, mDocView.getDisplayedViewIndex());
            edit.commit();
        }
    }

    public void onDestroy()
    {
        if (mDocView != null) {
            mDocView.applyToChildren(new ViewMapper() {
                void applyToView(View view) {
                    ((MuPDFView)view).releaseBitmaps();
                }
            });
        }
        if (core != null)
            core.onDestroy();
        if (mAlertTask != null) {
            mAlertTask.cancel(true);
            mAlertTask = null;
        }
        core = null;
        super.onDestroy();
    }

    private void setButtonEnabled(ImageButton button, boolean enabled) {
        button.setEnabled(enabled);
        button.setColorFilter(enabled ? Color.argb(255, 255, 255, 255):Color.argb(255, 128, 128, 128));
    }

    private void showButtons() {
        if (core == null)
            return;
        if (!mButtonsVisible) {
            mButtonsVisible = true;
            // Update page number text and slider
            int index = mDocView.getDisplayedViewIndex();
            updatePageNumView(index);
            mPageSlider.setMax((core.countPages()-1)*mPageSliderRes);
            mPageSlider.setProgress(index*mPageSliderRes);
            if (mTopBarMode == TopBarMode.Search) {
                mSearchText.requestFocus();
                showKeyboard();
            }
            slideUpToVisible(mBottomBarSwitcher);
            slideDownToVisible(mTopBarSwitcher);
            mPageNumberView.setVisibility(View.VISIBLE);
        }
    }

    private void hideButtons() {
        if (mButtonsVisible) {
            mButtonsVisible = false;
            hideKeyboard();
            slideUpToHide(mTopBarSwitcher);
            slideDownToHide(mBottomBarSwitcher);
            mPageNumberView.setVisibility(View.INVISIBLE);
        }
    }

    private void showPopMenu(){
        bookselectmenu.setVisibility(View.VISIBLE);
        bookselecttextup.setVisibility(View.VISIBLE);
        bookselecttextdown.setVisibility(View.VISIBLE);
        mDocView.setMode(MuPDFReaderView.Mode.Selecting);
    }

    private void hidePopMenu(){
        bookselectmenu.setVisibility(View.INVISIBLE);
        bookselecttextup.setVisibility(View.INVISIBLE);
        bookselecttextdown.setVisibility(View.INVISIBLE);
        mDocView.setMode(MuPDFReaderView.Mode.Viewing);
    }

    private void searchModeOn() {
        if (mTopBarMode != TopBarMode.Search) {
            mTopBarMode = TopBarMode.Search;
            mSearchText.requestFocus();
            showKeyboard();
            mSearchBar.setVisibility(View.VISIBLE);
        }
    }

    private void searchModeOff() {
        if (mTopBarMode == TopBarMode.Search) {
            mTopBarMode = TopBarMode.Main;
            hideKeyboard();
            SearchTaskResult.set(null);
            mDocView.resetupChildren();
            mSearchBar.setVisibility(View.GONE);
        }
    }

    private void updatePageNumView(int index) {
        if (core == null)
            return;
        mPageNumberView.setText(String.format("%d / %d", index+1, core.countPages()));
    }

    /**
     * 打印PDF
     * **/
//    private void printDoc() {
//        if (!core.fileFormat().startsWith("PDF")) {
//            showInfo(getString(R.string.format_currently_not_supported));
//            return;
//        }
//
//        Intent myIntent = getIntent();
//        Uri docUri = myIntent != null ? myIntent.getData() : null;
//
//        if (docUri == null) {
//            showInfo(getString(R.string.print_failed));
//        }
//
//        if (docUri.getScheme() == null)
//            docUri = Uri.parse("file://"+docUri.toString());
//
//        Intent printIntent = new Intent(this, PrintDialogActivity.class);
//        printIntent.setDataAndType(docUri, "aplication/pdf");
//        printIntent.putExtra("title", mFileName);
//        startActivityForResult(printIntent, PRINT_REQUEST);
//    }
//
    private void showInfo(String message) {
        mInfoView.setText(message);

        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentApiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            SafeAnimatorInflater safe = new SafeAnimatorInflater((Activity)this, R.animator.info, (View)mInfoView);
        } else {
            mInfoView.setVisibility(View.VISIBLE);
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    mInfoView.setVisibility(View.INVISIBLE);
                }
            }, 500);
        }
    }

    private void makeButtonsView() {
        mButtonsView = getLayoutInflater().inflate(R.layout.mupdf_main,null);
        mBottomBarSwitcher = (ViewAnimator)mButtonsView.findViewById(R.id.idBottomBar);
        mPageSlider = (SeekBar)mButtonsView.findViewById(R.id.pageSlider);
        mPageNumberView = (TextView)mButtonsView.findViewById(R.id.pageNumber);
        mInfoView = (TextView)mButtonsView.findViewById(R.id.info);
        mFilenameView = (TextView)mButtonsView.findViewById(R.id.idFileName);
        mTopBarSwitcher = (ViewAnimator)mButtonsView.findViewById(R.id.idTopBar);
        mSearchBar = (LinearLayout) mButtonsView.findViewById(R.id.idSearchBar);
        mSearchBack = (ImageButton)mButtonsView.findViewById(R.id.searchBack);
        mSearchFwd = (ImageButton)mButtonsView.findViewById(R.id.searchForward);
        mSearchText = (EditText)mButtonsView.findViewById(R.id.searchText);
        mAcceptSwitcher = (ViewAnimator)mButtonsView.findViewById(R.id.annotationConfirm);
        mInfoView.setVisibility(View.INVISIBLE);
        mAcceptSwitcher.setVisibility(View.INVISIBLE);

        bookselectmenu = (RelativeLayout)mButtonsView.findViewById(R.id.bookselectmenu);//主菜单
        bookselecttextup = (RelativeLayout)mButtonsView.findViewById(R.id.bookselecttextup);//上箭头
        bookselecttextdown = (RelativeLayout)mButtonsView.findViewById(R.id.bookselecttextdown);//下箭头

        annotationselectmenu = (RelativeLayout)mButtonsView.findViewById(R.id.idMuPDFPopHit);//批注外包盒子
        annotationselectmenu.setVisibility(View.INVISIBLE);

        hidePopMenu();
    }


    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.showSoftInput(mSearchText, 0);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
    }

    private void search(int direction) {
        hideKeyboard();
        int displayPage = mDocView.getDisplayedViewIndex();
        SearchTaskResult r = SearchTaskResult.get();
        int searchPage = r != null ? r.pageNumber : -1;
        mSearchTask.go(mSearchText.getText().toString(), direction, displayPage, searchPage);
    }

    @Override
    public boolean onSearchRequested() {
        if (mButtonsVisible && mTopBarMode == TopBarMode.Search) {
            hideButtons();
        } else {
            showButtons();
            searchModeOn();
        }
        return super.onSearchRequested();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mButtonsVisible && mTopBarMode != TopBarMode.Search) {
            hideButtons();
        } else {
            showButtons();
            searchModeOff();
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onStart() {
        if (core != null)
        {
            core.startAlerts();
            createAlertWaiter();
        }

        super.onStart();
    }

    @Override
    protected void onStop() {
        if (core != null)
        {
            destroyAlertWaiter();
            core.stopAlerts();
        }

        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (core != null && core.hasChanges()) {
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (which == AlertDialog.BUTTON_POSITIVE)
                        core.save();

                    finish();
                }
            };
            AlertDialog alert = mAlertBuilder.create();
            alert.setTitle("MuPDF");
            alert.setMessage(getString(R.string.document_has_changes_save_them_));
            alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes), listener);
            alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.no), listener);
            alert.show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void performPickFor(FilePicker picker) {
        mFilePicker = picker;
//        Intent intent = new Intent(this, ChoosePDFActivity.class);
//        intent.setAction(ChoosePDFActivity.PICK_KEY_FILE);
//        startActivityForResult(intent, FILEPICK_REQUEST);
    }

    /************ Animate Tools ************/
    //向上滑动以显示
    public void slideUpToVisible(final ViewAnimator v){
        v.setVisibility(View.INVISIBLE);
        Animation anim = new TranslateAnimation(0,0, v.getHeight(),0);
        anim.setDuration(200);
        anim.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) {
                v.setVisibility(View.VISIBLE);
            }
            public void onAnimationRepeat(Animation animation) {}
            public void onAnimationEnd(Animation animation) {}
        });
        v.startAnimation(anim);
    }
    //向上滑动以隐藏
    public void slideUpToHide(final ViewAnimator v){
        if(v.getVisibility() == View.INVISIBLE){
            return;
        }
        Animation anim = new TranslateAnimation(0,0,0, -v.getHeight());
        anim.setDuration(200);
        anim.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) {}
            public void onAnimationRepeat(Animation animation) {}
            public void onAnimationEnd(Animation animation) {
                v.setVisibility(View.INVISIBLE);
            }
        });
        v.startAnimation(anim);
    }
    //向下滑动以显示
    public void slideDownToVisible(final ViewAnimator v){
        Animation anim = new TranslateAnimation(0,0, -v.getHeight(),0);
        anim.setDuration(200);
        anim.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) {
                v.setVisibility(View.VISIBLE);
            }
            public void onAnimationRepeat(Animation animation) {}
            public void onAnimationEnd(Animation animation) {}
        });
        v.startAnimation(anim);
    }
    //向下滑动以隐藏
    public void slideDownToHide(final ViewAnimator v){
        if(v.getVisibility() == View.GONE){
            return;
        }
        Animation anim = new TranslateAnimation(0,0,0, v.getHeight());
        anim.setDuration(200);
        anim.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) {}
            public void onAnimationRepeat(Animation animation) {}
            public void onAnimationEnd(Animation animation) {
                v.setVisibility(View.GONE);
            }
        });
        v.startAnimation(anim);
    }

    /******** mupdf_accept.xml ********/

    /**
     * 批注
     * **/
    public void onPizhuClick(View v){
        hideButtons();
        slideUpToVisible(mAcceptSwitcher);
        mTopBarMode = TopBarMode.Accept;
        mDocView.setMode(MuPDFReaderView.Mode.Drawing);
        showInfo(getString(R.string.draw_annotation));
    }
    /**
     * 保存批注
     * **/
    public void onAnnotationSave(View v){
        MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
        if (pageView != null) {
            if (!pageView.saveDraw())
                showInfo(getString(R.string.nothing_to_save));
        }

        mDocView.setMode(MuPDFReaderView.Mode.Viewing);
        slideDownToHide(mAcceptSwitcher);
        mTopBarMode = TopBarMode.Main;
    }
    /**
     * 取消刚刚画的批注
     * **/
    public void onAnnotationCancel(View v){
        MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
        if (pageView != null) {
            pageView.cancelDraw();
        }

        mDocView.setMode(MuPDFReaderView.Mode.Viewing);
        slideDownToHide(mAcceptSwitcher);
        mTopBarMode = TopBarMode.Main;
    }
    /**
     * 防止点击事件冒泡
     * **/
    public void onBubbling(View v){

    }


    /******** mupdf_topbar.xml ********/

    /**
     * 返回上一页
     * **/
    public void onFinishActivity(View v){
        finish();
    }

    /**
     * 打开搜索
     * **/
    public void OnOpenSearchButtonClick(View v){
        searchModeOn();
    }

    /**
     * 关闭搜索
     * **/
    public void OnCancelSearchButtonClick(View v){
        searchModeOff();
    }

    /**
     * 下一页
     * **/
    public void onSmartMoveBackwards(View v){
        mDocView.smartMoveBackwards();
    }

    /**
     * 上一页
     * **/
    public void onSmartMoveForwards(View v){
        mDocView.smartMoveForwards();
    }



    /******** mupdf_pop_menu.xml ********/

    /**
     * 添加下划线
     * **/
    public void onUnderlineSave(View v){
        MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
        if (pageView != null)
            pageView.markupSelection(Annotation.Type.UNDERLINE);

        hidePopMenu();
    }

    /**
     * 添加高亮
     * **/
    public void onHighlightSave(View v){
        MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
        if (pageView != null)
            pageView.markupSelection(Annotation.Type.HIGHLIGHT);

        hidePopMenu();
    }

    /**
     * 搜索所选文本
     * **/
    public void onSearchTextToBaidu(View v){
        MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
        if (pageView != null)
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.search_engine)+pageView.getSelectedString().toString())));

        hidePopMenu();
    }

    /**
     * 复制所选文本
     * **/
    public void onCopyTextSave(View v){
        MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
        if (pageView != null)
            pageView.copySelection();

        showInfo(getString(R.string.copy_text_to_the_clipboard));
        hidePopMenu();
    }

    /**
     * 取消
     * **/
    public void onCancelSave(View v){
        MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
        if (pageView != null)
            pageView.deselectText();

        hidePopMenu();
    }

    /******** mupdf_pop_hit.xml ********/

    /**
     * 删除所选批注
     * **/
    public void onDeleteSelectedAnnotation(View v){
        MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
        if (pageView != null)
            pageView.deleteSelectedAnnotation();
    }
}