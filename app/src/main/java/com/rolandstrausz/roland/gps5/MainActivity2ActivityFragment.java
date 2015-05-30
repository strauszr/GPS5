package com.rolandstrausz.roland.gps5;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;



/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivity2ActivityFragment extends Fragment {


    private Button startScreenButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        startScreenButton=(Button) getActivity().findViewById(R.id.button_activity_start);

        startScreenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), BigScreenActivity.class);
                startActivity(intent);
            }

            ;
        });
        return inflater.inflate(R.layout.fragment_main_activity2, container, false);
    }
}





