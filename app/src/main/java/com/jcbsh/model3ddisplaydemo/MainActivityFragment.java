package com.jcbsh.model3ddisplaydemo;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private TextView mCurrentMode;

    public static Fragment getInstance() {

        Fragment fragment = new MainActivityFragment();
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_main, container, false);
        Button mRecordVideoButton = (Button) v.findViewById(R.id.model_button);
        mRecordVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), Model3DActivity.class);
                startActivity(intent);
            }
        });

        mCurrentMode = (TextView) v.findViewById(R.id.mode_textView);
        switch (Model3D.sMode) {
            case Model3D.SINGLE_THREAD:
                mCurrentMode.setText("single thread");
                break;
            case Model3D.MULTI_THREAD:
                mCurrentMode.setText("multi thread");
                break;
        }


        Button singleThreadButton = (Button) v.findViewById(R.id.single_thread_button);
        singleThreadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Model3D.sMode = Model3D.SINGLE_THREAD;
                mCurrentMode.setText("single thread");

            }
        });

        Button multiThreadButton = (Button) v.findViewById(R.id.multi_thread_button);
        multiThreadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Model3D.sMode = Model3D.MULTI_THREAD;
                mCurrentMode.setText("multi thread");
            }
        });



        return v;
    }

}
