package org.thosp.yourlocalweather.service;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;

import org.thosp.yourlocalweather.utils.AppPreference;

@TargetApi(Build.VERSION_CODES.M)
public class StartAppAlarmJob extends AbstractAppJob {
    private static final String TAG = "StartAppAlarmJob";

    private JobParameters params;
    int connectedServicesCounter;

    @Override
    public boolean onStartJob(JobParameters params) {
        this.params = params;
        connectedServicesCounter = 0;
        String updateAutoPeriodStr = AppPreference.getLocationAutoUpdatePeriod(getBaseContext());
        if (!"OFF".equals(updateAutoPeriodStr)) {
            reScheduleNextAlarm(1, updateAutoPeriodStr, StartAutoLocationJob.class);
        }
        String updatePeriodStr = AppPreference.getLocationUpdatePeriod(getBaseContext());
        if (!"0".equals(updateAutoPeriodStr)) {
            reScheduleNextAlarm(2, updatePeriodStr, StartRegularLocationJob.class);
        }
        reScheduleNextAlarm(3, AppPreference.getInterval(getBaseContext()), StartNotificationJob.class);
        Intent intent = new Intent(this, ScreenOnOffUpdateService.class);
        bindService(intent, screenOnOffUpdateServiceConnection, Context.BIND_AUTO_CREATE);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        unbindService(screenOnOffUpdateServiceConnection);
        unbindAllServices();
        return true;
    }

    @Override
    protected void serviceConnected(ServiceConnection serviceConnection) {
        connectedServicesCounter++;
        if (connectedServicesCounter >= 4) {
            jobFinished(params, false);
        }
    }

    private ServiceConnection screenOnOffUpdateServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            ScreenOnOffUpdateService.ScreenOnOffUpdateServiceBinder binder =
                    (ScreenOnOffUpdateService.ScreenOnOffUpdateServiceBinder) service;
            ScreenOnOffUpdateService screenOnOffUpdateService = binder.getService();
            screenOnOffUpdateService.startSensorBasedUpdates(0);
            new Thread(new Runnable() {
                public void run() {
                    serviceConnected(screenOnOffUpdateServiceConnection);
                }
            }).start();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };
}