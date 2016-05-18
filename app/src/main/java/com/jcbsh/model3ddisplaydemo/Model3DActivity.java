package com.jcbsh.model3ddisplaydemo;

import android.app.Fragment;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by JCBSH on 18/04/2016.
 */
public class Model3DActivity extends SingleFragmentActivity implements Model3DFragment.FragmentCallback{

    private ProgressBar mPercentageBar;
    private TextView mPercentageText;
    private View mProgressContainer;

    @Override
    protected Fragment createFragment() {
        return Model3DFragment.getInstance();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_model;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mProgressContainer = findViewById(R.id.progressContainer);
        mPercentageBar = (ProgressBar) findViewById(R.id.percentage_bar);
        mPercentageText = (TextView) findViewById(R.id.percentage_textView);

//        mPercentageBar.setProgress(50)
    }

    @Override
    public void setPBarVisibility(boolean b) {

        if (b) {
            mProgressContainer.setVisibility(View.VISIBLE);
        } else {
            mProgressContainer.setVisibility(View.INVISIBLE);
        }

    }

    @Override
    public boolean isPBarVisibility() {
        if (mProgressContainer.getVisibility() == View.VISIBLE) return true;
        return false;
    }
}
