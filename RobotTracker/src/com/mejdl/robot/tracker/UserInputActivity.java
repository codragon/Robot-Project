package com.mejdl.robot.tracker;

import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.view.Window;
import android.widget.EditText;

public class UserInputActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_user_input);
    }
    
    public void onClick(View view)
    {
    	EditText cols = (EditText) findViewById(R.id.edit_message);
    	EditText rows = (EditText) findViewById(R.id.edit_message1);
    	
    	if(cols != null && rows != null){
    		GlobalVar.TemplateWidth = Integer.parseInt(cols.getText().toString());
    		GlobalVar.TemplateHeight = Integer.parseInt(rows.getText().toString());
    	}
    	
    	finish();

    }

}
