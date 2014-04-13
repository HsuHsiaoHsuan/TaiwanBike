package idv.funnybrain.bike;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.google.android.gms.maps.model.LatLng;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

/**
 * Created by Freeman on 2014/2/12.
 */
public class Utils {

    public static final String OPENDATA_BIKE = "http://www.c-bike.com.tw/xml/stationlistopendata.aspx";

    public static final String OPENDATA_BIKE_TAIPEI = "http://210.69.61.60:8080/you/gwjs_cityhall.json";

    public static final String StationID = "StationID";
    public static final String StationNO = "StationNO";
    public static final String StationPic = "StationPic";
    public static final String StationPic2 = "StationPic2";
    public static final String StationPic3 = "StationPic3";
    public static final String StationMap = "StationMap";
    public static final String StationName = "StationName";
    public static final String StationAddress = "StationAddress";
    public static final String StationLat = "StationLat";
    public static final String StationLon = "StationLon";
    public static final String StationDesc = "StationDesc";
    public static final String StationNums1 = "StationNums1";
    public static final String StationNums2 = "StationNums2";
    public static final String fromList = "fromList";
    public static final String fromFavorList = "fromFavorList";
    public static final String fromMap = "fromMap";

    public static final String travelModeDriving = "driving";
    public static final String travelModeWalking = "walking";
    public static final String travelModeBicycling = "bicycling";
    public static final String travelModeTransit = "transit";

    public static final String travelModeDrivingUpper = "DRIVING";
    public static final String travelModeWalkingUpper = "WALKING";
    public static final String travelModeBicyclingUpper = "BICYCLING";
    public static final String travelModeTransitUpper = "TRANSIT";

    private static Location CurrentLocation = null;
    private final String API_KEY = "AIzaSyD0zl7nMTR2jsoAWMOFn0xTA_qFE-hgu8E";
    public static final String navOK = "OK";
    public static final String navNOT_FOUND = "NOT_FOULD";
    public static final String navZERO_RESULT = "ZERO_RESULT";
    public static final String navINVALID_REQUEST = "INVALID_REQUEST";
    public static final String navOVER_REQUEST_LINIT = "OVER_QUERY_LIMIT";
    public static final String navREQUEST_DENIED = "REQUEST_DENIED";
    public static final String navUNKNOWN_ERROR = "UNKNOWN_ERROR";

    private static final String MapSourceMode = "MapSourceMode";
    private static final String sourceMode = "mode";

    public synchronized void setLocation(Location location) {
        CurrentLocation = location;
    }

    public Location getLocation() {
        return CurrentLocation;
    }

    public static synchronized DefaultHttpClient getHttpClient() {
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 15000); // Socket will open for 15 secs.
        HttpConnectionParams.setSoTimeout(httpParams, 15000); // Connection will live for 15 secs to wait response.
        return new DefaultHttpClient(httpParams);
    }

    public boolean checkNetworkAvailable(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if(networkInfo != null && networkInfo.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    public void setMapSourceMode(Context context, int mode) {
        SharedPreferences source_mode = context.getSharedPreferences(MapSourceMode, 0);
        SharedPreferences.Editor editor = source_mode.edit();
        editor.putInt(sourceMode, mode);
        editor.commit();
    }

    public int getMapSourceMode(Context context) {
        SharedPreferences source_mode = context.getSharedPreferences(MapSourceMode, 0);
        return source_mode.getInt(sourceMode, 9);
    }

    public String getDirectionURL(Location from, LatLng to, String travelMode) {
        String origin = "origin=" + from.getLatitude() + "," + from.getLongitude();
        String destination = "destination=" + to.latitude + "," + to.longitude;
        String sensor = "sensor=false";
        String mode= "mode=" + travelMode;
        //String avoid = "avoid=tolls|highways|ferries";
        //String region = "region=zh-Hant-TW";

        String result = "https://maps.googleapis.com/maps/api/directions/json?" +
                        origin + "&" + destination + "&" + sensor + "&" + mode + "&" + /*avoid + "&" +*/ "key=" + API_KEY;

        if (travelMode.equals(travelModeTransit)) {
            long time = System.currentTimeMillis() / 1000;
            String dep_time = "departure_time=" + String.valueOf(time);
            String result_transit = "https://maps.googleapis.com/maps/api/directions/json?" +
                    origin + "&" + destination + "&" + sensor + "&" + mode + "&" + dep_time + "&" + "key=" + API_KEY;
            return result_transit;
        }

        return result;
    }
}
