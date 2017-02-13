package tu.pccc.pccc;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private String deviceId = "";
    private Socket mSocket;

    {
        try {
            IO.Options options = new IO.Options();
            options.forceNew = true;
            options.reconnection = true;

            //mSocket = IO.socket("http://103.48.83.139:8899", options);
           mSocket = IO.socket("http://192.168.43.108:8899", options);
            mSocket.connect();

        } catch (URISyntaxException e) {
            Log.e("ERROR", e.getMessage());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        JSONObject json = new JSONObject();
        try {
            json.put("MarkerId", deviceId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mSocket.emit("AndroidDisconnected", json);
    }

    private void emitWhenOnline() {
        //Emit Current place
        JSONObject json = new JSONObject();
        try {
            json.put("MarkerId", deviceId);
            json.put("isFire", currentFireState);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mSocket.emit("AndroidConnected", json);
    }

    private TextView txt;
    private EditText txtDeviceId;
    private Button btnSimulateFire;
    private Button btnDeviceId;
    private boolean currentFireState = false;


    private void initLayout() {
        //edtIpAddress = (EditText) findViewById(R.id.edtIpAddress);
        txt = (TextView) findViewById(R.id.txt);
        btnSimulateFire = (Button) findViewById(R.id.btnSimulateFire);
        btnSimulateFire.setOnClickListener(this);
        btnDeviceId = (Button) findViewById(R.id.btnDeviceId);
        btnDeviceId.setOnClickListener(this);
        txtDeviceId = (EditText) findViewById(R.id.txtDeviceId);

        editFireState();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initLayout();
    }

    private void editFireState() {
        if (currentFireState) {
            txt.setText("MakerId ID: " + deviceId + " - On Fire");
            btnSimulateFire.setText("Disable");
        } else {
            txt.setText("MakerId ID: " + deviceId + " - Click to simulate Fire alert");
            btnSimulateFire.setText("Simulate");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mSocket.connected()) {
            mSocket.connect();
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == btnSimulateFire.getId()) {
            currentFireState = !currentFireState;
            JSONObject json = new JSONObject();
            try {
                json.put("MarkerId", deviceId);
                json.put("isFire", currentFireState);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            mSocket.emit("AndroidFireStateChanged", json);
            editFireState();
        } else if (view.getId() == btnDeviceId.getId()) {
            deviceId = txtDeviceId.getText().toString().trim();
            btnSimulateFire.setVisibility(View.VISIBLE);
            txt.setVisibility(View.VISIBLE);
            btnDeviceId.setVisibility(View.GONE);
            txtDeviceId.setVisibility(View.GONE);
            editFireState();
            emitWhenOnline();
        }
    }
}
