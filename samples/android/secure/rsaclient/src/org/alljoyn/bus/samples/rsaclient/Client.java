/*
 * This sample is identical to the simple sample, with the addition of security.  Refer to the
 * simple sample for further explanation of the AllJoyn code not called out here.
 *
 * Copyright 2010-2011, Qualcomm Innovation Center, Inc.
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.alljoyn.bus.samples.rsaclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.alljoyn.bus.AuthListener;
import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.FindNameListener;
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.Status;

import java.util.concurrent.CountDownLatch;

public class Client extends Activity {
    static {
        System.loadLibrary("alljoyn_java");
    }
    
    private static final int DIALOG_CREATE_PASSPHRASE = 1;
    private static final int DIALOG_ENTER_PASSPHRASE = 2;

    private static final int MESSAGE_PING = 1;
    private static final int MESSAGE_PING_REPLY = 2;
    private static final int MESSAGE_GET_CREDENTIALS = 3;
    private static final int MESSAGE_AUTH_COMPLETE = 4;
    private static final int MESSAGE_POST_TOAST = 5;

    private static final String TAG = "SecureRsaClient";

    private EditText mEditText;
    private ArrayAdapter<String> mListViewArrayAdapter;
    private ListView mListView;
    private Menu menu;
    private int mCredentialsDialog;
    private CountDownLatch mLatch;
    private String mPassword;
    
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_PING:
                String ping = (String) msg.obj;
                mListViewArrayAdapter.add("Ping:  " + ping);
                break;
            case MESSAGE_PING_REPLY:
                String ret = (String) msg.obj;
                mListViewArrayAdapter.add("Reply:  " + ret);
                mEditText.setText("");
                break;
            case MESSAGE_GET_CREDENTIALS:
                AuthListener.PasswordRequest request = (AuthListener.PasswordRequest) msg.obj;
                mCredentialsDialog = request.isNewPassword() 
                    ? DIALOG_CREATE_PASSPHRASE : DIALOG_ENTER_PASSPHRASE;
                showDialog(mCredentialsDialog);
                break;
            case MESSAGE_AUTH_COMPLETE:
                Boolean authenticated = (Boolean) msg.obj;
                if (authenticated.equals(Boolean.FALSE)) {
                    Toast.makeText(Client.this, "Authentication failed", Toast.LENGTH_LONG).show();
                }
                break;
            case MESSAGE_POST_TOAST:
            	Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_LONG).show();
            	break;
            default:
                break;
            }
        }
    };
    
    /* The authentication listener for our bus attachment. */
    private RsaKeyXListener mAuthListener;

    private BusHandler mBusHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mListViewArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mListView = (ListView) findViewById(R.id.ListView);
        mListView.setAdapter(mListViewArrayAdapter);

        mEditText = (EditText) findViewById(R.id.EditText);
        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_NULL
                        && event.getAction() == KeyEvent.ACTION_UP) {
                        Message msg = mBusHandler.obtainMessage(BusHandler.PING, 
                                                                view.getText().toString());
                        mBusHandler.sendMessage(msg);
                    }
                    return true;
                }
            });

        HandlerThread busThread = new HandlerThread("BusHandler");
        busThread.start();
        mBusHandler = new BusHandler(busThread.getLooper());

        mAuthListener = new RsaKeyXListener();
        mBusHandler.sendEmptyMessage(BusHandler.CONNECT);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        this.menu = menu;
        return true;
    }
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.quit:
	    	finish();
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        mBusHandler.sendEmptyMessage(BusHandler.DISCONNECT);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_CREATE_PASSPHRASE:
        case DIALOG_ENTER_PASSPHRASE:
            LayoutInflater factory = LayoutInflater.from(this);
            View view = factory.inflate(R.layout.passphrase_dialog, null);
            int title = (id == DIALOG_CREATE_PASSPHRASE)
                ? R.string.create_passphrase_dialog : R.string.enter_passphrase_dialog;
            EditText editText = (EditText) view.findViewById(R.id.PasswordEditText);
            editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_NULL
                            && event.getAction() == KeyEvent.ACTION_UP) {
                            mPassword = view.getText().toString();
                            mLatch.countDown();
                            dismissDialog(mCredentialsDialog);
                        }
                        return true;
                    }
                });
            return new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setCancelable(false)
                .setView(view)
                .create();
        default:
            return null;
        }
    }

    private void sendUiMessage(int what, Object obj) {
        mHandler.sendMessage(mHandler.obtainMessage(what, obj));
    }

    /*
     * The main differences between a secure application and a plain application, besides the
     * @Secure annotations of the interfaces, are encapsulated in the AuthListener.  The
     * BusAttachment calls the listener with various authentication requests in the process of
     * authenticating a peer.  The requests made are dependent on the specific authentication
     * mechanism negotiated between the peers.
     */
    class RsaKeyXListener implements AuthListener {

        /* Returns the list of supported mechanisms. */
        public String getMechanisms() {
            return "ALLJOYN_RSA_KEYX";
        }

        /* 
         * Persistent authentication and encryption data is stored at this location.  
         * 
         * This uses the private file area associated with the application package.
         */
        public String getKeyStoreFileName() {
            return getFileStreamPath("alljoyn_keystore").getAbsolutePath();
        }

        /*
         * Authentication requests are being made.  Contained in this call are the mechanism in use,
         * the number of attempts made so far, the desired user name for the requests, and the
         * specific credentials being requested in the form of AuthRequests.
         *
         * The ALLJOYN_RSA_KEYX mechanism will request the passphrase, certificate chain, and private
         * key, and to verify the certificate chain of the peer.
         */
        public boolean requested(String authMechanism, int count, String userName,
                                 AuthRequest[] requests) {
            /* Collect the requests we're interested in to simplify processing below. */
            PasswordRequest passwordRequest = null;
            CertificateRequest certificateRequest = null;
            VerifyRequest verifyRequest = null;
            for (AuthRequest request : requests) {
                if (request instanceof PasswordRequest) {
                    passwordRequest = (PasswordRequest) request;
                } else if (request instanceof CertificateRequest) {
                    certificateRequest = (CertificateRequest) request;
                } else if (request instanceof VerifyRequest) {
                    verifyRequest = (VerifyRequest) request;
                }
            }

            if (verifyRequest != null) {
                /* Verify a certificate chain supplied by the peer. */
                return true;
            } else if (certificateRequest != null) {
                /* 
                 * The engine is asking us for our certificate chain.  
                 *
                 * If we return true and do not supply the certificate chain, then the engine will
                 * create a self-signed certificate for us.  It will ask for the passphrase to use
                 * for the private key via a PasswordRequest. 
                 */
                return true;
            } else if (passwordRequest != null) {
                /*
                 * A password request under the ALLJOYN_RSA_KEYX mechanism is for the passphrase of the
                 * private key.
                 *
                 * PasswordRequest.isNewPassword() indicates if the engine has created a private key
                 * for us (as part of creating a self-signed certificate).  Otherwise it is
                 * expecting the passphrase for the existing private key.
                 */
                try {
                    if (count <= 3) {
                        /*
                         * Request the passphrase of our private key via the UI.  We need to wait
                         * here for the user to enter the passphrase before we can return.  The
                         * latch takes care of the synchronization for us.
                         */
                        mLatch = new CountDownLatch(1);
                        sendUiMessage(MESSAGE_GET_CREDENTIALS, passwordRequest);
                        mLatch.await();
                        passwordRequest.setPassword(mPassword.toCharArray());
                        return true;
                    }
                } catch (InterruptedException ex) {
                    Log.e(TAG, "Error waiting for passphrase", ex);
                }
            }
            return false;
        }

        public void completed(String authMechanism, boolean authenticated) {
            sendUiMessage(MESSAGE_AUTH_COMPLETE, authenticated);
        }
    }
    
    class BusHandler extends Handler {
        
        private static final String SERVICE_NAME = "org.alljoyn.bus.samples.secure";

        private BusAttachment mBus;
        private ProxyBusObject mProxyObj;
        private SecureInterface mSecureInterface;
        
        public static final int CONNECT = 1;
        public static final int DISCONNECT = 2;
        public static final int PING = 3;

        public BusHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
            case CONNECT: {
                mBus = new BusAttachment(getClass().getName(), BusAttachment.RemoteMessage.Receive);

                /*
                 * Register the AuthListener before calling connect() to ensure that everything is
                 * in place before any remote peers access the service.
                 */
                Status status = mBus.registerAuthListener(mAuthListener.getMechanisms(), 
                                                          mAuthListener,
                                                          mAuthListener.getKeyStoreFileName());
                logStatus("BusAttachment.registerAuthListener()", status);
                if (status != Status.OK) {
                    finish();
                    return;
                }

                status = mBus.connect();
                logStatus("BusAttachment.connect()", status);
                if (Status.OK != status) {
                    finish();
                    return;
                }
                
                mProxyObj = mBus.getProxyBusObject(SERVICE_NAME, "/SecureService", 
                                                   new Class[] { SecureInterface.class });

                mSecureInterface = mProxyObj.getInterface(SecureInterface.class);

                status = mBus.findName(SERVICE_NAME, new FindNameListener() {
                        public void foundName(String name, String guid, String namePrefix, 
                                              String busAddress) {
                            Status status = mProxyObj.connect(busAddress);
                            logStatus("ProxyBusObject.connect()", status);
                            if (status != Status.OK) {
                                finish();
                                return;
                            }
                            
                            mBus.cancelFindName(SERVICE_NAME);
                            logStatus("BusAttachment.cancelFindName()", status);
                            if (status != Status.OK) {
                                finish();
                                return;
                            }
                        }

                        public void lostAdvertisedName(String name, String guid, String namePrefix, String busAddr) { }
                    });
                logStatus("BusAttachment.findName()", status);
                if (status != Status.OK) {
                    finish();
                    return;
                }
                break;
            }
            
            case DISCONNECT: {
                mProxyObj.disconnect();
                mBus.disconnect();
                getLooper().quit();
                break;
            }
            
            case PING: {
                try {
                    sendUiMessage(MESSAGE_PING, msg.obj);
                    String reply = mSecureInterface.Ping((String) msg.obj);
                    sendUiMessage(MESSAGE_PING_REPLY, reply);
                } catch (BusException ex) {
                    logException("SecureInterface.Ping()", ex);
                }
                break;
            }
            default:
                break;
            }
        }        
    }

    private void logStatus(String msg, Status status) {
        String log = String.format("%s: %s", msg, status);
        if (status == Status.OK) {
            Log.i(TAG, log);
        } else {
        	Message toastMsg = mHandler.obtainMessage(MESSAGE_POST_TOAST, log);
            mHandler.sendMessage(toastMsg);
            Log.e(TAG, log);
        }
    }

    private void logException(String msg, BusException ex) {
        String log = String.format("%s: %s", msg, ex);
        Message toastMsg = mHandler.obtainMessage(MESSAGE_POST_TOAST, log);
        mHandler.sendMessage(toastMsg);
        Log.e(TAG, log, ex);
    }
}
