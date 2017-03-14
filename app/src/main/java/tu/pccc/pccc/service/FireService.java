package tu.pccc.pccc.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import tu.pccc.pccc.FullscreenActivity;
import tu.pccc.pccc.R;
import tu.pccc.pccc.receiver.HeadsetReceiver;

/**
 * Created by ThanhND on 3/1/17.
 */

public class FireService extends Service {

    private void registerHeadSetPlug(){
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(new HeadsetReceiver(), filter);
        Log.d("SAC", "ZOOO");
    }

    @Override
    public void onCreate() {
        Log.d("SAC", "Service on create");
        Intent notificationIntent = new Intent(this, FullscreenActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("PCCC")
                .setContentText("Service đang chạy")
                .setContentIntent(pendingIntent).build();

        startForeground(1234, notification);
        registerHeadSetPlug();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("SAC", "Service onStartCommand");

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
