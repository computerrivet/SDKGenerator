package com.playfab;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

// This example built from:
// http://stackoverflow.com/questions/20097506/using-the-new-android-advertiser-id-inside-an-sdk


// <summary>
// Fetch and expose the Google Advertising Id, and expose it to the SDK
// </summary>
public final class PlayFabGetAdvertId {
    // <summary>
    // A container class for the data
    // </summary>
    public static final class AdInfo {
        public final String advertisingId;
        public final boolean limitAdTrackingEnabled;

        AdInfo(String advertisingId, boolean limitAdTrackingEnabled) {
            this.advertisingId = advertisingId;
            this.limitAdTrackingEnabled = limitAdTrackingEnabled;
        }
    }

    // <summary>
    // Perform the work that fetches and stores the advertisingId
    // </summary>
    public static AdInfo getAdvertisingIdInfo(Context context) throws Exception {
        if(Looper.myLooper() == Looper.getMainLooper()) {
            Log.e("MYAPP", "Do not attempt to call getAdvertisingIdInfo the main thread.  It will cause your program to hang.");
            throw new IllegalStateException("Cannot be called from the main thread");
        }

        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo("com.android.vending", 0);
        } catch(Exception e) {
            Log.e("MYAPP", "exception", e);
            throw e;
        }

        AdvertisingConnection connection = new AdvertisingConnection();
        Intent intent = new Intent("com.google.android.gms.ads.identifier.service.START");
        intent.setPackage("com.google.android.gms");
        try {
            if(context.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
                AdvertisingInterface adInterface = new AdvertisingInterface(connection.getBinder());
                AdInfo adInfo = new AdInfo(adInterface.getId(), adInterface.isLimitAdTrackingEnabled(true));
                return adInfo;
            }
        } catch(Exception e) {
            Log.e("MYAPP", "exception", e);
            throw e;
        } finally {
            context.unbindService(connection);
        }
        throw new IOException("Google Play connection failed");
    }

    // <summary>
    // A basic wrapper for connecting to a service - Used specifically for "...android.gms.ads.identifier.service" in this case
    // </summary>
    private static final class AdvertisingConnection implements ServiceConnection {
        boolean retrieved = false;
        private final LinkedBlockingQueue<IBinder> queue = new LinkedBlockingQueue<IBinder>(1);

        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                this.queue.put(service);
            } catch (InterruptedException e) {
                Log.e("MYAPP", "exception", e);
            }
        }

        public void onServiceDisconnected(ComponentName name){}

        public IBinder getBinder() throws InterruptedException {
            if (this.retrieved) throw new IllegalStateException();
            this.retrieved = true;
            return (IBinder)this.queue.take();
        }
    }

    // <summary>
    // Low level work that extracts and stores the data stored in the AdInfo object
    // </summary>
    private static final class AdvertisingInterface implements IInterface {
        private IBinder binder;

        public AdvertisingInterface(IBinder pBinder) {
            binder = pBinder;
        }

        public IBinder asBinder() {
            return binder;
        }

        public String getId() throws RemoteException {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            String id;
            try {
                data.writeInterfaceToken("com.google.android.gms.ads.identifier.internal.IAdvertisingIdService");
                binder.transact(1, data, reply, 0);
                reply.readException();
                id = reply.readString();
            } catch(Exception e) {
                Log.e("MYAPP", "exception", e);
                throw e;
            } finally {
                reply.recycle();
                data.recycle();
            }
            return id;
        }

        public boolean isLimitAdTrackingEnabled(boolean paramBoolean) throws RemoteException {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            boolean limitAdTracking;
            try {
                data.writeInterfaceToken("com.google.android.gms.ads.identifier.internal.IAdvertisingIdService");
                data.writeInt(paramBoolean ? 1 : 0);
                binder.transact(2, data, reply, 0);
                reply.readException();
                limitAdTracking = 0 != reply.readInt();
            } catch(Exception e) {
                Log.e("MYAPP", "exception", e);
                throw e;
            } finally {
                reply.recycle();
                data.recycle();
            }
            return limitAdTracking;
        }
    }
}
