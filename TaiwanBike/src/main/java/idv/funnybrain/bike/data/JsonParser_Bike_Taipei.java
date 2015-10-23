package idv.funnybrain.bike.data;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import idv.funnybrain.bike.Utils;
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
import java.util.Iterator;
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
            final String location = httpURLConnection.getHeaderField("Location");
            httpURLConnection.disconnect();

            URL newURL = new URL(location);
            HttpURLConnection newConn = (HttpURLConnection) newURL.openConnection();
            newConn.setReadTimeout(10000);
            newConn.setConnectTimeout(15000);
            newConn.connect();

            inputStream = newConn.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            StringBuffer sb = new StringBuffer();
            String line = "";
            while ( (line = br.readLine()) != null ) {
                sb.append(line);
            }
            data = sb.toString();

            br.close();
            inputStream.close();
            newConn.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            resultFromTaipeiGovernment = data;
        }

        try {
            JSONObject allObject = new JSONObject(resultFromTaipeiGovernment);

//            String retVal_data = allObject.getString("retVal");
            JSONObject obj_result = allObject.getJSONObject("retVal");
            JsonFactory factory = new JsonFactory();
//            JsonParser parser = factory.createParser(retVal_data);
            JsonParser parser;
            ObjectMapper mapper = new ObjectMapper();
            mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            Iterator<String> it = obj_result.keys();
            while (it.hasNext()) {
                final String idx = it.next();
                String result = obj_result.getString(idx);
                parser = factory.createParser(result);
                Station_Taipei station = mapper.readValue(parser, Station_Taipei.class);
                station_list.add(station);
                station_hashmap.put(station.getID(), station);
            }
//            Station_Taipei[] stationList = mapper.readValue(parser, Station_Taipei[].class);
//            for(Station_Taipei st : stationList) {
//                station_list.add(st);
//                station_hashmap.put(st.getID(), st);
//            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public List<IStation> getStations() {
//        return station_list;
//    }

    public HashMap<String, IStation> getStationHashMap() {
        return station_hashmap;
    }

    /*
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
                stationLat = input.getString("lat").replaceAll("\\p{C}", ""); if(D) { Log.d(TAG, "lat: " + stationLat); }
                stationLng = input.getString("lng").replaceAll("\\p{C}", ""); if(D) { Log.d(TAG, "lng: " + stationLng); }
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
    }*/
}
