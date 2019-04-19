package adrienguillement;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import adrienguillement.issoverhead.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private Double longitude;
    private Double latitude;
    private long date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            finish();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            finish();
        }

        LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        longitude = location.getLongitude();
        latitude = location.getLatitude();

        // Call the api
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if(info != null && info.isConnected())
        {
            Log.i("ISS","Appel de la tâche asynchrone qui va se connecter au web Service Ok");
            AccesRessourceTask task = new AccesRessourceTask();
            task.execute();
        }
        else
        {
            Toast.makeText(MainActivity.this, "Pas internet", Toast.LENGTH_SHORT).show();
        }

        // Notify the time when the iss pass over head
        Button addNotification = (Button) findViewById(R.id.addNotification);
        addNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NotificationHelper notificationHelper = new NotificationHelper(getApplicationContext());

                Timestamp timestamp = new Timestamp(date);
                String dateIRL = new SimpleDateFormat("dd/MM/yyyy H:m").format(new Date(timestamp.getTime()*1000L));

                Log.d("fergerger", dateIRL + ":" + String.valueOf(timestamp));
                notificationHelper.createNotification("Eh bien c'est très simple fred : ", dateIRL);
            }
        });
    }

    private class AccesRessourceTask extends AsyncTask<String, Void, String>
    {
        @Override
        protected String doInBackground(String... strings)
        {
            Log.i("ISS","Entrée dans le doInBackground de la tache asynchrone");
            return getData();
        }

        @Override
        protected void onPostExecute(String s)
        {
            Log.d("ISS", s);
        }
    }

    private String getData() {
        HttpURLConnection httpUrlConnection = null;

        StringBuffer stringBuffer = new StringBuffer();
        String urlString = "http://api.open-notify.org/iss-pass.json?lat="+ latitude +"&lon=" + longitude;
        Log.d("fezfzefzef", urlString);
        Log.d("EEEEE", urlString);

        try {
            Log.i("ISS","Création de l'objet URL");
            URL url = new URL(urlString);

            Log.i("ISS","Création de l'objet HttpURLConnection & Envoi de la requête");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            Log.i("ISS","Récupération de la réponse");
            InputStream in = connection.getInputStream();
            InputStreamReader isr = new InputStreamReader(in);

            int unCharacter;

            while((unCharacter = isr.read()) != -1)
            {
                stringBuffer.append((char)unCharacter);
            }

            connection.disconnect();
        }
        catch(Exception ex)
        {
            Log.e("ISS","ERREUR : " + ex.getMessage());
        }

        Log.i("ISS","Résultat : " + stringBuffer.toString());
        Log.i("ISS","Résultat : " + stringBuffer.toString());
        try {
            parseJson(stringBuffer.toString());
        } catch (Exception e){

        }
        return stringBuffer.toString();
    }

    private void parseJson(String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);
        JSONArray movies = jsonObject.getJSONArray("response");

        JSONObject movie = movies.getJSONObject(0);
        date = Integer.valueOf(movie.getString("risetime"));
    }

    public class NotificationHelper {

        private Context mContext;
        private NotificationManager mNotificationManager;
        private NotificationCompat.Builder mBuilder;
        public static final String NOTIFICATION_CHANNEL_ID = "10001";

        public NotificationHelper(Context context) {
            mContext = context;
        }

        /**
         * Create and push the notification
         */
        public void createNotification(String title, String message)
        {
            /**Creates an explicit intent for an Activity in your app**/
            Intent resultIntent = new Intent(mContext , MainActivity.class);
            resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            PendingIntent resultPendingIntent = PendingIntent.getActivity(mContext,
                    0 /* Request code */, resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            mBuilder = new NotificationCompat.Builder(mContext);
            mBuilder.setSmallIcon(R.mipmap.ic_launcher);
            mBuilder.setContentTitle(title)
                    .setContentText(message)
                    .setAutoCancel(false)
                    .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                    .setContentIntent(resultPendingIntent);

            mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
            {
                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "NOTIFICATION_CHANNEL_NAME", importance);
                notificationChannel.enableLights(true);
                notificationChannel.setLightColor(Color.RED);
                notificationChannel.enableVibration(true);
                notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
                assert mNotificationManager != null;
                mBuilder.setChannelId(NOTIFICATION_CHANNEL_ID);
                mNotificationManager.createNotificationChannel(notificationChannel);
            }
            assert mNotificationManager != null;
            mNotificationManager.notify(0 /* Request Code */, mBuilder.build());
        }
    }
}
