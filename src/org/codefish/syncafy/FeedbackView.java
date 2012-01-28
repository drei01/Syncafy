/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.codefish.syncafy;

/**
 * activity for sending feedback
 * @author Matthew
 */
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import org.codefish.syncafy.utils.AndroidUtils;

/**
 * activity for feedback form
 * @author Matthew
 */
public class FeedbackView extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.feedback_form);
    }

    public void sendFeedback(View view) {
        final EditText nameField = (EditText) findViewById(R.id.EditTextName);
        String name = nameField.getText().toString();

        final EditText emailField = (EditText) findViewById(R.id.EditTextTwitter);
        String email = emailField.getText().toString();

        final EditText feedbackField = (EditText) findViewById(R.id.EditTextFeedbackBody);
        String feedback = feedbackField.getText().toString();

        final Spinner feedbackSpinner = (Spinner) findViewById(R.id.SpinnerFeedbackType);
        String feedbackType = feedbackSpinner.getSelectedItem().toString();


        //create a new intent to send the email feedback
        AndroidUtils.createEmail(getApplicationContext(), new String[]{this.getString(R.string.email_address,"unknown")},
                new String[0], new String[0], "Syncafy Feedback: "+feedbackType, "email:"+email+"\n\r"+feedback+"\n\r"+name);

        finish();//finish the activity
    }
}
