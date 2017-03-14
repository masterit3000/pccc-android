package tu.pccc.pccc;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.List;

import tu.pccc.pccc.db.sqlite.TblLogDAO;
import tu.pccc.pccc.db.sqlite.TblSmsDAO;
import tu.pccc.pccc.receiver.HeadsetReceiver;
import tu.pccc.pccc.service.FireService;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private static final int REQUEST_READ_PHONE_STATE_PERMISSION = 1111;
    private static final int REQUEST_SEND_SMS_PERMISSION = 2222;
    private static final int HANDLER_REGISTER_DEVICE_COMPLETED = 1;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private TextView txtIMEI = null;
    private TextView txtStatus = null;

    private boolean currentFireState = false;
    private int PRESSING_STATE_NORMAL = 0;
    private int PRESSING_STATE_PRESSING_DOWN = 1;
    private int PRESSING_STATE_RELEASH_UP = 2;

    private int pressingState = PRESSING_STATE_NORMAL;


    private String deviceId = "";
    private Socket mSocket;

    {
        try {
            IO.Options options = new IO.Options();
            options.forceNew = true;
            options.reconnection = true;

            mSocket = IO.socket(SysConstraints.SOCKET_URL, options);
            mSocket.connect();

        } catch (URISyntaxException e) {
            Log.e("ERROR", e.getMessage());
        }
    }

    //Khi may da duoc duyet thanh cong & add device thanh cong
    private void emitWhenOnline() {
        if (deviceId.length() > 0) {
            //Emit Current place
            JSONObject json = new JSONObject();
            try {
                json.put("MarkerId", deviceId);
                json.put("isFire", currentFireState);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            mSocket.emit("AndroidConnected", json);
            registerHeadSetPlug();

            //Log.d("SAC","Try start service");
            //Start fire service
            // Intent intent = new Intent(this, FireService.class);
            //startService(intent);

        }
    }

    private void registerHeadSetPlug() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(new HeadsetReceiver(), filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        emitWhenOnline();
        if (!mSocket.connected()) {
            mSocket.connect();
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
        if (mSocket != null) {
            mSocket.emit("AndroidDisconnected", json);
        }

    }


    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    private final int STATUS_WAITING_FOR_REVIEW = 0;
    private final int STATUS_APPROVED = 1;
    private final int STATUS_RUNNING = 2;
    private final int STATUS_NOT_APPROVED = 3;

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case HANDLER_REGISTER_DEVICE_COMPLETED:
                    Object[] o = (Object[]) message.obj;
                    if ((boolean) o[0]) {
                        JsonObject json = (JsonObject) o[1];
                        JsonObject jsonDevice = json.getAsJsonObject("device");
                        if (jsonDevice != null) {
                            JsonElement eMarkerId = json.get("markerId");
                            JsonElement eName = json.get("name");
                            JsonArray sms = json.getAsJsonArray("sms");
                            int status = jsonDevice.get("status").getAsInt();
                            switch (status) {
                                case STATUS_WAITING_FOR_REVIEW:
                                    txtStatus.setText("Chờ duyệt");
                                    break;
                                case STATUS_APPROVED:
                                    txtStatus.setText("Đã duyệt, chưa gán địa điểm");
                                    break;
                                case STATUS_RUNNING:
                                    deviceId = eMarkerId.getAsString();
                                    txtStatus.setText("Đang chạy: " + eMarkerId.getAsString() + " - " + eName.getAsString());

                                    CF.write(FullscreenActivity.this.getApplicationContext(), SysConstraints.KEY_MARKER_ID, eMarkerId.getAsString());
                                    CF.write(FullscreenActivity.this.getApplicationContext(), SysConstraints.KEY_NAME, eName.getAsString());

                                    TblSmsDAO tblSmsDAO = new TblSmsDAO(FullscreenActivity.this.getApplicationContext());
                                    tblSmsDAO.insertSms(sms.toString());
                                    emitWhenOnline();
                                    break;
                                case STATUS_NOT_APPROVED:
                                    txtStatus.setText("Máy không được duyệt");
                                    break;
                                default:
                                    break;
                            }

                        }
                    } else {
                        txtStatus.setText(o[1].toString());
                    }
                    break;
                default:
                    break;
            }
            return false;
        }
    });

    /***
     * When file state changed send to server to notify
     */
    private void emitFireStateChanged() {
        if (deviceId.length() > 0) {

            JSONObject json = new JSONObject();
            try {
                json.put("MarkerId", deviceId);
                json.put("isFire", currentFireState);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (mSocket != null) {
                mSocket.emit("AndroidFireStateChanged", json);
            }

            //Insert log
            TblLogDAO tblLogDAO = new TblLogDAO(this.getApplicationContext());
            tblLogDAO.insertLog("Cảnh báo cháy", currentFireState ? "Đã bật" : "Đã tắt");

            //Send SMS to preset sms users on databases
            TblSmsDAO tblSmsDAO = new TblSmsDAO(this.getApplicationContext());
            List<String> allSMSToSend = tblSmsDAO.getAllSMSToSend();
            for (int i = 0; i < allSMSToSend.size(); i++) {
                String smsContent = "";
                String markerId = CF.read(this.getApplicationContext(), SysConstraints.KEY_MARKER_ID, "");
                String markerName = CF.read(this.getApplicationContext(), SysConstraints.KEY_NAME, "");

                if (currentFireState) {
                    smsContent = "Cảnh báo cháy tại " + markerName + " (" + markerId + ") Đã bật";
                } else {
                    smsContent = "Cảnh báo cháy tại " + markerName + " (" + markerId + ") Đã tắt";
                }
                CF.sendSMS(allSMSToSend.get(i), smsContent);
            }

        }

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();

        switch (keyCode) {
            case KeyEvent.KEYCODE_HEADSETHOOK: {
                if (action == KeyEvent.ACTION_DOWN) {
                    if (deviceId.length() > 0) {
                        currentFireState = true;
                        if (pressingState == PRESSING_STATE_NORMAL) {
                            emitFireStateChanged();
                            pressingState = PRESSING_STATE_PRESSING_DOWN;
                        }
                    }
                } else {
                    if (action == KeyEvent.ACTION_UP) {
                        if (deviceId.length() > 0) {
                            currentFireState = false;
                            emitFireStateChanged();
                            pressingState = PRESSING_STATE_NORMAL;
                        }
                    }
                }
                return true;
            }
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    if (deviceId.length() > 0) {
                        currentFireState = true;
                        if (pressingState == PRESSING_STATE_NORMAL) {
                            emitFireStateChanged();
                            pressingState = PRESSING_STATE_PRESSING_DOWN;
                        }
                    }
                } else {
                    if (action == KeyEvent.ACTION_UP) {
                        if (deviceId.length() > 0) {
                            currentFireState = false;
                            emitFireStateChanged();
                            pressingState = PRESSING_STATE_NORMAL;
                        }
                    }
                }
                return true;
            default:

                return super.dispatchKeyEvent(event);
        }
    }

    private void registerDevice() {
        String imei = CF.read(this.getApplicationContext(), SysConstraints.KEY_IMEI, "");
        if (imei.length() > 0) {
            JsonObject json = new JsonObject();
            json.addProperty("imei", imei);
            json.addProperty("manufacture", Build.MANUFACTURER);
            json.addProperty("deviceName", Build.MODEL);

            Ion.with(getApplicationContext())
                    .load(SysConstraints.SERVICE_URL + "/InitDeviceRoute/RegisterDevice")
                    .basicAuthentication(SysConstraints.BASIC_AUTH_USERNAME, SysConstraints.BASIC_AUTH_PASSWORD)
                    .setJsonObjectBody(json)
                    .asJsonObject()

                    .setCallback(new FutureCallback<JsonObject>() {
                        @Override
                        public void onCompleted(Exception e, JsonObject result) {

                            if (e != null) {
                                Object[] obj = new Object[2];
                                final Message message = handler.obtainMessage();
                                obj[0] = false;
                                obj[1] = "Không thể kết nối đến server: " + e.getMessage();
                                message.what = HANDLER_REGISTER_DEVICE_COMPLETED;
                                message.obj = obj;
                                handler.sendMessage(message);
                            } else {
                                Object[] obj = new Object[2];
                                final Message message = handler.obtainMessage();
                                obj[0] = true;
                                obj[1] = result;
                                message.what = HANDLER_REGISTER_DEVICE_COMPLETED;
                                message.obj = obj;
                                handler.sendMessage(message);
                            }
                        }

                    });

        } else {
            txtIMEI.setText("Không thể đọc imei");
        }


    }

    @SuppressLint("HardwareIds")
    private void initLayout() {
        txtIMEI = (TextView) findViewById(R.id.txtIMEI);
        txtStatus = (TextView) findViewById(R.id.txtStatus);
        // Check if we're running on Android 5.0 or higher
        if (Build.VERSION.SDK_INT >= 23) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);

            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.SEND_SMS},
                        REQUEST_READ_PHONE_STATE_PERMISSION);
            } else {
                registerDevice();
                txtIMEI.setText("Imei\n" + CF.read(this.getApplicationContext(), SysConstraints.KEY_IMEI, ""));
            }

        } else {
            registerDevice();
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            String deviceId = telephonyManager.getDeviceId();
            txtIMEI.setText("Imei\n" + deviceId);
            CF.write(this.getApplicationContext(), SysConstraints.KEY_IMEI, deviceId);
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_READ_PHONE_STATE_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                    String deviceId = telephonyManager.getDeviceId();
                    txtIMEI.setText("Imei:\n" + deviceId);
                    CF.write(this.getApplicationContext(), SysConstraints.KEY_IMEI, deviceId);
                    registerDevice();
                } else {
                    txtIMEI.setText("Chưa gán quyền cho máy, không thể đọc imei");
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);
        initLayout();


    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(0);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
