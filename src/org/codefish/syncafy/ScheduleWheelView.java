/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.codefish.syncafy;

/**
 *
 * @author Matthew
 */
import kankan.wheel.widget.WheelView;
import kankan.wheel.widget.adapters.ArrayWheelAdapter;
import kankan.wheel.widget.adapters.NumericWheelAdapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

/**
 * activity for scheduling scroll wheel
 * @author Matthew
 */
public class ScheduleWheelView extends Activity {

    private static WheelView numberWheel;
    private static WheelView unitWheel;
    private static final int TEXT_SIZE = 20;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.wheel_layout);

        // number wheel
        numberWheel = (WheelView) findViewById(R.id.number_wheel);
        NumericAdapter numAdapter = new NumericAdapter(this, 1, 30);
        numAdapter.setTextSize(TEXT_SIZE);
        numberWheel.setViewAdapter(numAdapter);

        // unit wheel
        String units[] = new String[]{"hours", "days", "months"};
        StringArrayAdapter stringAdapter = new StringArrayAdapter(this, units);
        stringAdapter.setTextSize(TEXT_SIZE);
        unitWheel = (WheelView) findViewById(R.id.unit_wheel);
        unitWheel.setViewAdapter(stringAdapter);
    }

    public void schedule(View view) {
        //show a short message to show that the schedule has been set
        Toast.makeText(super.getApplicationContext(), "Schedule set", Toast.LENGTH_SHORT).show();

        /*Use the following to send a result back
        Bundle stats = new Bundle();
        stats.putString("height", "6\'4\"");
        super.setResult(RESULT_OK, stats);
         */

        finish();//finish the activity
    }

    /**
     * Adapter for numeric wheels. Highlights the current value.
     */
    private class NumericAdapter extends NumericWheelAdapter {

        /**
         * Constructor
         */
        public NumericAdapter(Context context, int minValue, int maxValue) {
            super(context, minValue, maxValue);
            setTextSize(16);
        }

        @Override
        protected void configureTextView(TextView view) {
            super.configureTextView(view);
            view.setTypeface(Typeface.SANS_SERIF);
        }

        @Override
        public View getItem(int index, View cachedView, ViewGroup parent) {
            return super.getItem(index, cachedView, parent);
        }
    }

    /**
     * Adapter for string based wheel. Highlights the current value.
     */
    private class StringArrayAdapter extends ArrayWheelAdapter<String> {
        /**
         * Constructor
         */
        public StringArrayAdapter(Context context, String[] items) {
            super(context, items);
            setTextSize(16);
        }

        @Override
        protected void configureTextView(TextView view) {
            super.configureTextView(view);
            view.setTypeface(Typeface.SANS_SERIF);
        }

        @Override
        public View getItem(int index, View cachedView, ViewGroup parent) {
            return super.getItem(index, cachedView, parent);
        }
    }
}
