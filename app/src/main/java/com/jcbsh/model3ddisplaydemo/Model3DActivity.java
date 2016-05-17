package com.jcbsh.model3ddisplaydemo;

import android.app.Fragment;

/**
 * Created by JCBSH on 18/04/2016.
 */
public class Model3DActivity extends SingleFragmentActivity{

    @Override
    protected Fragment createFragment() {
        return Model3DFragment.getInstance();
    }
}
