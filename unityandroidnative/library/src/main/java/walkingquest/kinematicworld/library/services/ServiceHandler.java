package walkingquest.kinematicworld.library.services;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import walkingquest.kinematicworld.library.database.DatabaseAccessor;
import walkingquest.kinematicworld.library.database.contracts.StepLogContract;


/**
 * Created by Devon on 6/14/2017.
 */

public class ServiceHandler extends Service {

    private IBinder mBinder = new ServiceBinder();

    private StepCounterService mStepCounterService;
    private boolean stepCounterServiceRegistered;

    private TimerService mTimerService;
    private boolean timerServiceRegistered;

    private long steps;

    private DatabaseAccessor databaseAccessor;


    @Override
    public void onCreate(){

        Log.d("Unity", "Handler Service Create");

        steps = 0;

        // bind this service to the timer service to push and pull information from it
        Intent timerIntent = new Intent(this, TimerService.class);
        startService(timerIntent);
        bindService(timerIntent, timerServiceConnection, Context.BIND_AUTO_CREATE);

        // bind this service to the step counter service to pull information from it
        Intent stepIntent = new Intent(this, StepCounterService.class);
        startService(stepIntent);
        bindService(stepIntent, stepServiceConnection, Context.BIND_AUTO_CREATE);

        databaseAccessor = new DatabaseAccessor(getApplicationContext());

        Log.d("Unity", "Handler Service Created");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        unbindService(stepServiceConnection);
    }

    public void Update(){

    }

    public void Update(String msg){

        switch (msg){
            case "STEP":
                // record a step in the database
                SQLiteDatabase db = databaseAccessor.getWritableDatabase();
                db.insert(StepLogContract.StepEntry.TABLE_NAME, null, StepLogContract.StepCommands.AddEntry());
                db.close();
                steps++;
                break;
            default:
                break;
        }

        mTimerService.Update();

    }

    // get the count of steps in the step log
    public long getSteps(){
        SQLiteDatabase db = databaseAccessor.getReadableDatabase();
        long steps = StepLogContract.StepCommands.GetCompleteStepCount(db);
        db.close();

        return steps;
    }

    public void setSteps(long steps){
        Log.d("Unity", "Setting Steps " + steps);
        this.steps = steps;
    }



    // Below is required for binding services/activities

    public class ServiceBinder extends Binder {
        ServiceHandler getService() {
            // Return this instance of LocalService so clients can call public methods
            return ServiceHandler.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // create a connection between the stepcounter service and this
    private ServiceConnection stepServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            StepCounterService.ServiceBinder serviceBinder = (StepCounterService.ServiceBinder) iBinder;
            mStepCounterService = serviceBinder.getService();
            stepCounterServiceRegistered = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            stepCounterServiceRegistered = false;
        }
    };

    // create a connection between the stepcounter service and this
    private ServiceConnection timerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            TimerService.ServiceBinder serviceBinder = (TimerService.ServiceBinder) iBinder;
            mTimerService = serviceBinder.getService();
            timerServiceRegistered = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            timerServiceRegistered = false;
        }
    };
}