package hu.rycus.rpiomxremote;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import hu.rycus.rpiomxremote.RemoteService;

/**
 * Helper class to bind/unbind the remote service.
 *
 * <br/>
 * Created by Viktor Adam on 11/11/13.
 *
 * @author rycus
 */
public class RemoteServiceCreator implements ServiceConnection {

    /** The bound remote service instance. */
    protected RemoteService service;
    /** Is the service bound yet? */
    protected boolean serviceBound = false;
    /** Is bind requested already? */
    protected boolean bindRequested = false;

    /**
     * @see android.content.ServiceConnection
     *       #onServiceConnected(android.content.ComponentName, android.os.IBinder)
     */
    @Override
    public void onServiceConnected(ComponentName name, IBinder bnd) {
        RemoteService.RemoteBinder binder = (RemoteService.RemoteBinder) bnd;

        service = binder.getService();
        serviceBound = true;

        onServiceInstanceReceived(service);
    }

    /**
     * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
     */
    @Override
    public void onServiceDisconnected(ComponentName name) {
        serviceBound = false;
        service = null;
    }

    /** Returns true if the remote service is bound. */
    public boolean isServiceBound() { return serviceBound; }

    /** Returns true if bind is already requested. */
    public boolean isBindRequested() { return bindRequested; }

    /** Returns the bound service instance (or null if it isn't bound yet). */
    public RemoteService getService() { return service; }

    /** Method to execute when the service instance is bound. */
    protected void onServiceInstanceReceived(RemoteService service) {
        /* NO-OP by default */
    }

    /** Requests binding of the remote service. */
    public void bind(Context context) {
        context.getApplicationContext().bindService(new Intent(context, RemoteService.class), this, Context.BIND_AUTO_CREATE);
        bindRequested = true;
    }

    /** Requests unbinding the remote service. */
    public void unbind(Context context) {
        if(serviceBound) {
            context.getApplicationContext().unbindService(this);
        }
        bindRequested = false;
    }

}
