package org.cardioart.rbnb.client;

import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Source;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;

public class MainActivity extends ActionBarActivity implements Handler.Callback{

    private EditText editText;
    private TextView textViewConnectivityStatus;
    private TextView textViewTxSpeed;
    private static boolean isProxy = true;
    private Runnable mTimer;
    private Source mSource;
    private SpamThread spamThread;
    private long time_count=0;
    private Source source;

    private final static String TAG = "RBNB";
    private final Handler mHandler = new Handler(this);

    @Override
    public boolean handleMessage(Message message) {
        if (message.what == 0) {
            mHandler.post(mTimer);
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText = (EditText) findViewById(R.id.editText);
        textViewConnectivityStatus = (TextView) findViewById(R.id.textView1);
        textViewTxSpeed = (TextView) findViewById(R.id.textViewTxSpeed);

        if (isConnected()) {
            textViewConnectivityStatus.setText("Online");
            textViewConnectivityStatus.setBackgroundColor(Color.parseColor("#FF77E25F"));
            try {
                spamThread = new SpamThread(mHandler);
                spamThread.start();
            } catch (SAPIException e) {
                Log.d(TAG, "NOT CREATE SpamThread ");
                e.printStackTrace();
            }
        } else {
            textViewConnectivityStatus.setText("Offline");
            textViewConnectivityStatus.setBackgroundColor(Color.parseColor("#FFD9542F"));
        }
        mTimer = new Runnable() {
            @Override
            public void run() {
                time_count++;
                debugTxSpeed(spamThread.getByteSend() / time_count);
                mHandler.postDelayed(this, 1000);
            }
        };
        //new HttpAsyncTask().execute("http://hmkcode.com/examples/index.php");
        message("[start]");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        //mHandler.removeCallbacks(mTimer);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (spamThread != null) {
            spamThread.cancel();
            spamThread.interrupt();
        }
        super.onDestroy();
    }

    public void message(CharSequence text) {
        editText.append(text + "\n");
    }
    public static String GET(String url) {
        InputStream inputStream;
        String result = "";
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();

            if (isProxy) {
                HttpHost proxy = new HttpHost("proxy-sa.mahidol", 8080);
                httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
                httpClient.getCredentialsProvider().setCredentials(
                        new AuthScope("proxy-sa.mahidol", 8080),
                        new UsernamePasswordCredentials("u5413341", "g5u967b63"));
            }

            HttpResponse httpResponse = httpClient.execute(new HttpGet(url));
            inputStream = httpResponse.getEntity().getContent();
            if (inputStream != null) {
                result = getStringFromStream(inputStream);
            } else {
                result = "didn't work";
            }
        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }
        return result;
    }
    private static String getStringFromStream(InputStream inputStream) {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        String result = "";
        try {
            while ((line = bufferedReader.readLine()) != null) {
                result += line;
            }
            inputStream.close();
        } catch (IOException e) {
            Log.d("Convert", e.getLocalizedMessage());
        }
        return result;
    }
    public boolean isConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
    public void debugTxSpeed(long bytePerSecond) {
        double BPS = bytePerSecond / 1024;
        if (bytePerSecond < 1024) {
            textViewTxSpeed.setText(String.format("%d Bps", bytePerSecond));
        }
        else if (BPS > 0 && BPS < 1024) {
            textViewTxSpeed.setText(String.format("%.2f KBps", BPS));
        } else {
            BPS = BPS / 1024;
            if (BPS > 0 && BPS < 1024) {
                textViewTxSpeed.setText(String.format("%.2f MBps", BPS));
            } else {
                BPS = BPS / 1024;
                textViewTxSpeed.setText(String.format("%.2f GBps", BPS));
            }
        }
    }
    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return GET(urls[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getBaseContext(), "Received!", Toast.LENGTH_LONG);
            message(result);
        }
    }

}
