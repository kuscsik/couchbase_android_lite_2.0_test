package com.example.couchbase_test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    EditText docIdView;
    EditText carBrandView;
    private Button saveButton;
    private DbAdapter dbAdapter;
    private TextView readCarBrandView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbAdapter = new DbAdapter(this);

        docIdView = (EditText) findViewById(R.id.idEditText);
        carBrandView = (EditText) findViewById(R.id.carBrandTextView);
        saveButton = (Button) findViewById(R.id.saveButton);
        saveButton.setOnClickListener(this);
        readCarBrandView = (TextView) findViewById(R.id.textViewCarBrand);

        /**
         * Notifiers should not be used together with setting a document ID.
         * If we subscribe for a change it becomes readonly for us.
         * Bug in Couchbase?
         */
        dbAdapter.setBrandChangedNotifier("test", new DbAdapter.DbChangeNotifier() {
            @Override
            void brandUpdated(String brand) {
                Log.i("TAG", "Updated value: " + brand);
                readCarBrandView.setText(brand);
            }
        });
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == saveButton.getId()) {
            String id = docIdView.getText().toString();
            String brand = carBrandView.getText().toString();
            if ( id!=null && !id.isEmpty() && brand !=null && !brand.isEmpty()) {
                dbAdapter.saveBrand(id, brand);
            }
        }
    }
}
