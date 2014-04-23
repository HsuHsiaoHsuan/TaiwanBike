package idv.funnybrain.bike.data;

import android.util.Log;
import idv.funnybrain.bike.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Freeman on 2014/3/17.
 */
public class JsonParser_Bike_Taipei implements IParser {
    private static final boolean D = false;
    private static final String TAG = "JsonParser_Bike_Taipei";
    private String resultFromTaipeiGovernment;
    private List<IStation> station_list;
    private HashMap<String, IStation> station_hashmap;

    public JsonParser_Bike_Taipei() {
        station_list = new ArrayList<IStation>();
        station_hashmap = new HashMap<String, IStation>();
    }

    @Override
    public void downloadData() {
        InputStream inputStream = null;
        HttpURLConnection httpURLConnection = null;
        String data = "";

        try {
            URL url = new URL(Utils.OPENDATA_BIKE_TAIPEI);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.connect();

            inputStream = httpURLConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            StringBuffer sb = new StringBuffer();
            String line = "";
            while ( (line = br.readLine()) != null ) {
                sb.append(line);
            }
            data = sb.toString();

            br.close();
            inputStream.close();
            httpURLConnection.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            resultFromTaipeiGovernment = data;
        }

        try {
            JSONObject allObject = new JSONObject(resultFromTaipeiGovernment);
            JSONArray allStation = allObject.getJSONArray("retVal");
            for(int x=0; x<allStation.length(); x++) {
                Station tmpStation = new Station(allStation.getJSONObject(x));
                station_list.add(new Station(allStation.getJSONObject(x)));
                station_hashmap.put(tmpStation.getID(), tmpStation);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

//    public List<IStation> getStations() {
//        return station_list;
//    }

    public HashMap<String, IStation> getStationHashMap() {
        return station_hashmap;
    }

    public static class Station implements IStation {
        private String serialNo;
        private String stationName;
        private String availableBike;
        private String stationLat;
        private String stationLng;
        private String stationDistrict;
        private String stationAddress;
        private String availableParking;

        private String stationName_eng;
        private String stationDistrict_eng;
        private String stationAddress_eng;

        public Station(JSONObject input) {
            try {
                serialNo = input.getString("sno"); if(D) { Log.d(TAG, "sno: " + serialNo); }
                stationName = input.getString("sna"); if(D) { Log.d(TAG, "sna: " + stationName); }
                availableBike = input.getString("sbi"); if(D) { Log.d(TAG, "sbi: " + availableBike); }
                stationLat = input.getString("lat"); if(D) { Log.d(TAG, "lat: " + stationLat); }
                stationLng = input.getString("lng"); if(D) { Log.d(TAG, "lng: " + stationLng); }
                stationDistrict = input.getString("sarea"); if(D) { Log.d(TAG, "sarea: " + stationDistrict); }
                stationAddress = input.getString("ar"); if(D) { Log.d(TAG, "ar: " + stationAddress); }
                availableParking = input.getString("bemp"); if(D) { Log.d(TAG, "bemp: " + availableParking); }

                stationName_eng = input.getString("snaen"); if(D) { Log.d(TAG, "snaen: " + stationName_eng); }
                stationDistrict_eng = input.getString("sareaen"); if(D) { Log.d(TAG, "sareaen: " + stationDistrict_eng); }
                stationAddress_eng = input.getString("aren"); if(D) { Log.d(TAG, "aren: " + stationAddress_eng); }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public String getID() { return serialNo; }

        @Override
        public String getNO() {
            return serialNo;
        }

        @Override
        public String getPIC_SMALL() {
            return null;
        }

        @Override
        public String getPIC_MEDIUM() {
            return null;
        }

        @Override
        public String getPIC_LARGE() {
            return null;
        }

        public String getNAME() { return stationName; }

        public String getAVAILABLE_BIKE() { return availableBike; }

        public String getLAT() { return  stationLat; }

        public String getLON() { return stationLng; }

        public String getDistrict() { return stationDistrict; }

        public String getADDRESS() { return stationAddress; }

        public String getAVAILABLE_PARKING() { return availableParking; }

        public String getNAME_eng() { return stationName_eng; }

        public String getDistrict_eng() { return stationDistrict_eng; }

        public String getADDRESS_eng() { return stationAddress_eng; }
    }
}
