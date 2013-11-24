package hu.rycus.rpiomxremote;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import hu.rycus.rpiomxremote.RemoteService;

/**
 * Created by rycus on 11/11/13.
 */
public class RemoteServiceCreator implements ServiceConnection {

    protected RemoteService service;
    protected boolean serviceBound = false;
    protected boolean bindRequested = false;

    @Override
    public void onServiceConnected(ComponentName name, IBinder bnd) {
        RemoteService.RemoteBinder binder = (RemoteService.RemoteBinder) bnd;

        service = binder.getService();
        serviceBound = true;

        onServiceInstanceReceived(service);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        serviceBound = false;
        service = null;
    }

    public boolean isServiceBound() {
        return serviceBound;
    }

    public boolean isBindRequested() { return bindRequested; }

    public RemoteService getService() {
        return service;
    }

    protected void onServiceInstanceReceived(RemoteService service) {
        /* NO-OP by default */
    }

    public void bind(Context context) {
        context.getApplicationContext().bindService(new Intent(context, RemoteService.class), this, Context.BIND_AUTO_CREATE);
        bindRequested = true;
    }

    public void unbind(Context context) {
        if(serviceBound) {
            context.getApplicationContext().unbindService(this);
        }
        bindRequested = false;
    }

}
