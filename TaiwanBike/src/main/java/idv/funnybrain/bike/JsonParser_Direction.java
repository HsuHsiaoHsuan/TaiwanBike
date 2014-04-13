package idv.funnybrain.bike;

import android.util.Log;
import com.google.android.gms.maps.model.LatLng;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Freeman on 2014/3/14.
 */
public class JsonParser_Direction {
    private static final boolean D = false;
    private static final String TAG = "JsonParser_Direction";
    private final String resultFromGoogleDirection;
    private List<Steps> allSteps = new ArrayList<Steps>();

    private String error_result = "";
    private String distance_all = "";
    private String time_all = "";

    public JsonParser_Direction(String url_str) {
        InputStream inputStream = null;
        HttpURLConnection httpURLConnection = null;
        String data = "";


        try {
            URL url = new URL(url_str);
            //System.out.println(url_str);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.connect();
            inputStream = httpURLConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            StringBuffer sb = new StringBuffer();
            String line = "";
            while((line = br.readLine()) != null) {
                sb.append(line);
            }
            data = sb.toString();

            br.close();

            inputStream.close();
            httpURLConnection.disconnect();
        } catch(IOException ioe) {
            if(D) {ioe.printStackTrace();}
        } finally {
            if(D) { Log.d(TAG, "JsonParser_Direction: " + data); }
            resultFromGoogleDirection = data;
        }

        try {
            JSONObject allObject = new JSONObject(resultFromGoogleDirection);
            String status = allObject.getString("status");

            if(D) { Log.d(TAG, "direction api result status: " + status); }
            // TODO handle non-OK exception
            if(status.equals(Utils.navOK)) {
                JSONArray routesArray = allObject.getJSONArray("routes");
                JSONObject route_first = routesArray.getJSONObject(0);
                JSONArray legs = route_first.getJSONArray("legs");
                JSONObject overview_polyline = route_first.getJSONObject("overview_polyline");
                JSONArray warnings = route_first.getJSONArray("warnings");


                // legs
                JSONObject leg_first = legs.getJSONObject(0);
                JSONObject distance = leg_first.getJSONObject("distance");
                //String distance_string = distance.getString("text"); // TODO
                distance_all = distance.getString("text");
                JSONObject duration = leg_first.getJSONObject("duration");
                //String duration_string = duration.getString("text"); // TODO
                time_all = duration.getString("text");


                // steps
                JSONArray steps = leg_first.getJSONArray("steps");
                for(int x=0; x<steps.length(); x++) {
                    allSteps.add(new Steps(steps.getJSONObject(x)));
                }
            } else {
                error_result = status;
                Log.d(TAG, "JsonParser_Direction, status: " + status);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static class Steps {
        HashMap<String, String> distance = new HashMap<String, String>();
        HashMap<String, String> duration = new HashMap<String, String>();
        HashMap<String, String> end_location = new HashMap<String, String>();
        String html_instructions = "";
        HashMap<String, String> polyline = new HashMap<String, String>();
        HashMap<String, String> start_location = new HashMap<String, String>();
        List<Steps> steps_transit_only = null;
        Transit_Details transit_details = null;

        String travel_mode = "";

        public Steps(JSONObject object) {
            try {
                if (D) { Log.d(TAG, "object: " + object.toString()); }
                JSONObject dist = object.getJSONObject("distance"); if(D) { Log.d(TAG, dist.toString()); }
                distance.put("text", dist.getString("text")); if(D) { Log.d(TAG, dist.getString("text")); }
                distance.put("value", dist.getString("value")); if(D) { Log.d(TAG, dist.getString("value")); }

                JSONObject dur = object.getJSONObject("duration");
                duration.put("text", dur.getString("text")); if(D) { Log.d(TAG, dur.getString("text")); }
                duration.put("value", dur.getString("value")); if(D) { Log.d(TAG, dur.getString("value")); }

                JSONObject end = object.getJSONObject("end_location");
                end_location.put("lat", end.getString("lat")); if(D) { Log.d(TAG, end.getString("lat")); }
                end_location.put("lng", end.getString("lng")); if(D) { Log.d(TAG, end.getString("lng")); }

                html_instructions = object.getString("html_instructions"); if(D) { Log.d(TAG, html_instructions); }

                JSONObject poly = object.getJSONObject("polyline");
                polyline.put("points", poly.getString("points")); if(D) { Log.d(TAG, poly.getString("points")); }

                JSONObject start = object.getJSONObject("start_location");
                start_location.put("lat", start.getString("lat")); if(D) { Log.d(TAG, start.getString("lat")); }
                start_location.put("lng", start.getString("lng")); if(D) { Log.d(TAG, start.getString("lng")); }

                travel_mode = object.getString("travel_mode"); if(D) { Log.d(TAG, travel_mode); }
                //System.out.println("JsonParser travel_mode: " + travel_mode);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                JSONArray steps = object.getJSONArray("steps"); // transit mode only (walking path)
                if(steps != null) {
                    steps_transit_only = new ArrayList<Steps>();
                    for(int x=0; x<steps.length(); x++) {
                        steps_transit_only.add(new Steps(steps.getJSONObject(x)));
                    }
                }
            } catch(JSONException jsone) {
                jsone.printStackTrace();
            }
            try {
                transit_details = new Transit_Details(object.getJSONObject("transit_details"));
            } catch (JSONException jsone) {
                jsone.printStackTrace();
            }
        }

        public Transit_Details getTransitDetails() { return transit_details; }

        public List<LatLng> getDecodePoints() {
            String encoded = polyline.get("points");
            if(D) { Log.d(TAG, "getDecodePoints: " + encoded); }
            List<LatLng> poly = new ArrayList<LatLng>();
            int index = 0;
            int len = encoded.length();
            int lat = 0;
            int lng = 0;

            while (index < len) {
                int b, shift = 0, result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lat += dlat;

                shift = 0;
                result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lng += dlng;

                LatLng p = new LatLng((((double) lat / 1E5)),
                        (((double) lng / 1E5)));
                if(D) {Log.d(TAG, "decoded location: " + p.toString());}
                poly.add(p);
            }

            return poly;
        }
//        public List <LatLng> getDecodePoints(){
//            String encoded_points = polyline.get("points");
//            if(D) { Log.d(TAG, "getDecodePoints: " + encoded_points); }
//            int index = 0;
//            int lat = 0;
//            int lng = 0;
//            List <LatLng> out = new ArrayList<LatLng>();
//
//            try {
//                int shift;
//                int result;
//                while (index < encoded_points.length()) {
//                    shift = 0;
//                    result = 0;
//                    while (true) {
//                        int b = encoded_points.charAt(index++) - '?';
//                        result |= ((b & 31) << shift);
//                        shift += 5;
//                        if (b < 32)
//                            break;
//                    }
//                    lat += ((result & 1) != 0 ? ~(result >> 1) : result >> 1);
//
//                    shift = 0;
//                    result = 0;
//                    while (true) {
//                        int b = encoded_points.charAt(index++) - '?';
//                        result |= ((b & 31) << shift);
//                        shift += 5;
//                        if (b < 32)
//                            break;
//                    }
//                    lng += ((result & 1) != 0 ? ~(result >> 1) : result >> 1);
//                    /* Add the new Lat/Lng to the Array. */
//                    if(D) { Log.d(TAG, "getDecodePoints2: " + (new LatLng((lat*10),(lng*10)).toString())); }
//                    out.add(new LatLng((lat*10),(lng*10)));
//                }
//                return out;
//            }catch(Exception e) {
//                e.printStackTrace();
//            }
//            return out;
//        }
    }

    public List<Steps> getAllSteps() {
        return allSteps;
    }

    public String getAllDistance() { return distance_all; }

    public String getAllTime() { return time_all; }

    public String getErrorStatus() { return error_result; }


    public static class Transit_Details {
        String arrival_stop_name = null;
        //HashMap<String, Double> arrival_stop_location;

        String departure_stop_name = null;
        //HashMap<String, Double> departure_stop_location;

        //String transit_name = null;
        String transit_short_name = null;

        public Transit_Details(JSONObject input) {
            try {
                arrival_stop_name = input.getJSONObject("arrival_stop").getString("name");
                //System.out.println(arrival_stop_name);
                departure_stop_name = input.getJSONObject("departure_stop").getString("name");
                //System.out.println(departure_stop_name);
                //transit_name = input.getJSONObject("line").getString("name");
                //System.out.println(transit_name);
                transit_short_name = input.getJSONObject("line").getString("short_name");
                //System.out.println(transit_short_name);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public String getDepartureStop() {
            return departure_stop_name;
        }

        public String getArrivalStop() {
            return arrival_stop_name;
        }

//        public String getTransitName() {
//            return transit_name;
//        }

        public String getTransitShortName() {
            return transit_short_name;
        }
    }
}
