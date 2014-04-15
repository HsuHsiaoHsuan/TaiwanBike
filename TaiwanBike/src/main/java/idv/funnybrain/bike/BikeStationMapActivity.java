package idv.funnybrain.bike;

import android.content.*;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.*;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import idv.funnybrain.bike.data.IStation;
import idv.funnybrain.bike.data.JsonParser_Bike_Taipei;
import idv.funnybrain.bike.data.JsonParser_Direction;
import idv.funnybrain.bike.data.XmlParser_Bike;
import idv.funnybrain.bike.database.DBHelper;
import idv.funnybrain.bike.database.DBHelper_Taipei;
import org.jsoup.Jsoup;

import java.util.*;

/**
 * Created by Freeman on 2014/2/18.
 */
public class BikeStationMapActivity extends FragmentActivity implements GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {
    private static final boolean D = true;
    private static final String TAG = "BikeStationMapActivity";

    private GoogleMap mMap;

    private static int MAP_SOURCE_MODE = 1; // 0 for nothing, 1 for Kaohsiung, 2 for Taipei, 3 for both.

    //private static List<Marker> station_marker;
    private static HashMap<String, Marker> station_marker_hashmap = new HashMap<String, Marker>(); // for all(maybe Kaohsiung + Taipei)
    //private static SparseArray station_marker_array = new SparseArray();

    //static List<XmlParser_Bike.Station> station_list = new ArrayList<XmlParser_Bike.Station>(); // Kaohsiung City
    static List<IStation> station_list = new ArrayList<IStation>();
    static HashMap<String, IStation> station_hashmap = new HashMap<String, IStation>();

    private Handler handler;
    private final int MSG_DOWNLOAD_OK = 999;

    // for DrawerLayout
    //private String[] mStationTitles;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ListView mDrawerListRight;
    private ActionBarDrawerToggle mDrawerToggle;

    private static List<IXmlDownloader> listener = new ArrayList<IXmlDownloader>();

    private final static int
            CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private LocationClient mLocationClient;

    //private boolean isNetworkOK = false;
    private BroadcastReceiver mConnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (utils.checkNetworkAvailable(BikeStationMapActivity.this)) { // available
                //isNetworkOK = true;
                if(D) Log.d(TAG, "onReceive, networkAvailable check ok");
                // FIXME
                // TODO should check the status, than decide it's true of false
                getXML(false);
            } else { // not available
                //isNetworkOK = false;
                Toast.makeText(BikeStationMapActivity.this, R.string.check_netowrk, Toast.LENGTH_LONG).show();
            }
        }
    };

    SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            if(D) Log.d(TAG, "onQueryTextSubmit: " + query);
            ((BikeStationMapListAdapter) mDrawerList.getAdapter()).getFilter().filter(query);
            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            if(D) Log.d(TAG, "onQueryTextChange: " + newText);
            ((BikeStationMapListAdapter) mDrawerList.getAdapter()).getFilter().filter(newText);
            return true;
        }
    };

    private BikeStationMapActivity self;

    private final Utils utils = new Utils();

    private boolean isNavMode = false;

    private Menu myMenu;

    final int[] marker_num_array = {
            R.drawable.ic_marker_bike_0_v2,
            R.drawable.ic_marker_bike_1_v2,
            R.drawable.ic_marker_bike_2_v2,
            R.drawable.ic_marker_bike_3_v2,
            R.drawable.ic_marker_bike_4_v2,
            R.drawable.ic_marker_bike_5_v2,
            R.drawable.ic_marker_bike_6_v2,
            R.drawable.ic_marker_bike_7_v2,
            R.drawable.ic_marker_bike_8_v2,
            R.drawable.ic_marker_bike_9_v2,
            R.drawable.ic_marker_bike_9_plus_v2
    };

    final int[] marker_favor_num_array = {
            R.drawable.ic_marker_favor_0_v2,
            R.drawable.ic_marker_favor_1_v2,
            R.drawable.ic_marker_favor_2_v2,
            R.drawable.ic_marker_favor_3_v2,
            R.drawable.ic_marker_favor_4_v2,
            R.drawable.ic_marker_favor_5_v2,
            R.drawable.ic_marker_favor_6_v2,
            R.drawable.ic_marker_favor_7_v2,
            R.drawable.ic_marker_favor_8_v2,
            R.drawable.ic_marker_favor_9_v2,
            R.drawable.ic_marker_favor_9_plus_v2
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_bikemap);

        MAP_SOURCE_MODE = (new Utils()).getMapSourceMode(this);
        //Intent intent = getIntent();
        //MAP_SOURCE_MODE = intent.getIntExtra("MODE", 1);

        Intent intent = getIntent();
        int tmp = intent.getIntExtra("MODE", 9);
        if(tmp != 9) { MAP_SOURCE_MODE = tmp; } else {
            MAP_SOURCE_MODE = (new Utils()).getMapSourceMode(this);
        }


        self = this;

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        //mDrawerLayoutRight = (DrawerLayout) findViewById(R.id.right_drawer);
        //mDrawerLayoutRight.setDrawerShadow(R.drawable.drawer_shadow_right, Gravity.START);
        mDrawerListRight = (ListView) findViewById(R.id.right_drawer_list);

        // disable Toggle start
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //getSupportActionBar().setHomeButtonEnabled(true);
        // disable Toggle start
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                //supportInvalidateOptionsMenu();

                if(mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
                    myMenu.getItem(0).setIcon(R.drawable.ic_close_menu_left);
                }
                if(mDrawerLayout.isDrawerOpen(Gravity.RIGHT)) {
                    myMenu.getItem(2).setIcon(R.drawable.ic_close_menu_right);
                }
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                //System.out.println("onDrawerClosed");
                //supportInvalidateOptionsMenu();
                if(!mDrawerLayout.isDrawerOpen(Gravity.LEFT)) { // left is closed
                    myMenu.getItem(0).setIcon(R.drawable.ic_action_list);
                }
                if(!mDrawerLayout.isDrawerOpen(Gravity.RIGHT)) { // right is closed
                    myMenu.getItem(2).setIcon(R.drawable.ic_favor_list_v2);
                }
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        mLocationClient = new LocationClient(this, this, this);

        // setup SearchView
        SearchView searchView = (SearchView) findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(queryTextListener);

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case MSG_DOWNLOAD_OK:
                        if(D) Log.d(TAG, "handlMessage, I get message!");
                        Toast.makeText(BikeStationMapActivity.this, R.string.update_ok, Toast.LENGTH_LONG).show();
                        mMap.clear();

                        if(D) Log.d(TAG, "station_list length: " + station_hashmap.size());
                        Iterator<Map.Entry<String, IStation>> iterator = station_hashmap.entrySet().iterator();
                        while(iterator.hasNext()) {
                            Map.Entry<String, IStation> entry = iterator.next();
                            LatLng tmpLatLng = new LatLng(Double.valueOf(entry.getValue().getLAT()),
                                                          Double.valueOf(entry.getValue().getLON()));

                            boolean isFavor = false;
                            if(MAP_SOURCE_MODE == 2) { // taipei
                                DBHelper_Taipei db = new DBHelper_Taipei(self);
                                if(db.query(entry.getValue().getID())) { isFavor = true; }
                            } else {                   // kaohsiung
                                DBHelper db = new DBHelper(self);
                                if(db.query(entry.getValue().getID())) { isFavor = true; }
                            }

                            int icon = getMarkerIcon(Integer.valueOf(entry.getValue().getAVAILABLE_BIKE()), isFavor);

                            mMap.addMarker(new MarkerOptions().position(tmpLatLng)
                                                              .title(entry.getValue().getID())
                                                              .icon(BitmapDescriptorFactory.fromResource(icon))
                                                              .draggable(false));
                        }

                        if(mLocationClient.isConnected() && (mLocationClient.getLastLocation() != null)) { // get current location
                            utils.setLocation(mLocationClient.getLastLocation());
                        }
                        // set new station_list to left list
                        mDrawerList.setAdapter(new BikeStationMapListAdapter(getLayoutInflater(), new ArrayList<IStation>(station_hashmap.values())));
                        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

                        setUpMap();

                        setProgressBarIndeterminateVisibility(false);
                        break;
                }
            }
        };
        mDrawerLayout.findViewById(R.id.extraInfo_nav_exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setIsNavMode(false);
            }
        });
    }


    private int getMarkerIcon(int num, boolean isFavor) {
        if(isFavor) {
            if(num<10) {
                return marker_favor_num_array[num];
            } else {
                return marker_favor_num_array[10];
            }
        } else {
            if(num<10) {
                return marker_num_array[num];
            } else {
                return marker_num_array[10];
            }
        }
    }

    private class DrawerItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mDrawerLayout.closeDrawers();

            String selected_station_id = ((TextView) view.findViewById(R.id.site_id)).getText().toString();
            if(D) {
                Log.d(TAG, "you select the list position: " + position + ", but the Station.ID is " + selected_station_id);
            }

            LatLng focusLocation = new LatLng(Double.valueOf(station_hashmap.get(selected_station_id).getLAT()),
                                              Double.valueOf(station_hashmap.get(selected_station_id).getLON()));
            if(D) {
                Log.d(TAG, "selected id: " + ((TextView) view.findViewById(R.id.site_id)).getText() +
                           ", selected text: " + ((TextView) view.findViewById(R.id.title)).getText());
            }
            //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(focusLocation, 15.0f));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(focusLocation, 18.0f));

            mDrawerLayout.findViewById(R.id.extraInfo).setVisibility(View.VISIBLE);

            if(D) Log.d(TAG, "station_marker_hashmap legth: " + station_marker_hashmap.size());
        }
    }

    /**
    private class MyInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        private final View mWindow;

        MyInfoWindowAdapter() {
            mWindow = getLayoutInflater().inflate(R.layout.map_info_window, null);
        }
        @Override
        public View getInfoWindow(Marker marker) {
            render(marker, mWindow);

            // setting left extra info - start
            Iterator it = station_marker_hashmap.entrySet().iterator();
            while(it.hasNext()) {
                final Map.Entry pairs = (Map.Entry)it.next();
                if(((Marker)pairs.getValue()).equals(marker)) {
                    final String stationTitle = marker.getTitle();

                    if(MAP_SOURCE_MODE == 2) { // taipei
                        DBHelper_Taipei dbHelper_taipei = new DBHelper_Taipei(self);
                        if(dbHelper_taipei.query(pairs.getKey().toString())) {
                            ((ImageButton)mDrawerLayout.findViewById(R.id.extraInfo_favor_control)).setImageResource(R.drawable.ic_remove_favor);
                            mDrawerLayout.findViewById(R.id.extraInfo_favor_control).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    DBHelper_Taipei dbHelper_taipei = new DBHelper_Taipei(self);
                                    int result = dbHelper_taipei.delete(pairs.getKey().toString());
                                    if(result > 0) {
                                        refreshFavorMarker(pairs.getKey().toString(), false);
                                    }
                                }
                            });
                        } else {
                            ((ImageButton)mDrawerLayout.findViewById(R.id.extraInfo_favor_control)).setImageResource(R.drawable.ic_add_favor);
                            mDrawerLayout.findViewById(R.id.extraInfo_favor_control).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    DBHelper_Taipei dbHelper_taipei = new DBHelper_Taipei(self);
                                    ContentValues values = new ContentValues();
                                    values.put(DBHelper_Taipei.DB_COL_STATION_ID, pairs.getKey().toString());
                                    long result = dbHelper_taipei.insert(values);
                                    if (result > 0) {
                                        refreshFavorMarker(pairs.getKey().toString(), true);
                                    }
                                }
                            });
                        }

                    } else { // kaohsiung
                        DBHelper dbHelper = new DBHelper(self);
                        if(dbHelper.query(pairs.getKey().toString())) {
                            ((ImageButton)mDrawerLayout.findViewById(R.id.extraInfo_favor_control)).setImageResource(R.drawable.ic_remove_favor);
                            mDrawerLayout.findViewById(R.id.extraInfo_favor_control).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    DBHelper dbHelper = new DBHelper(self);
                                    int result = dbHelper.delete(pairs.getKey().toString());
                                    if(result > 0) {
                                        refreshFavorMarker(pairs.getKey().toString(), false);
                                    }
                                }
                            });
                        } else {
                            ((ImageButton)mDrawerLayout.findViewById(R.id.extraInfo_favor_control)).setImageResource(R.drawable.ic_add_favor);
                            mDrawerLayout.findViewById(R.id.extraInfo_favor_control).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    ContentValues values = new ContentValues();
                                    values.put(DBHelper_Taipei.DB_COL_STATION_ID, pairs.getKey().toString());
                                    DBHelper dbHelper = new DBHelper(self);
                                    long result = dbHelper.insert(values);
                                    if (result > 0) {
                                        refreshFavorMarker(pairs.getKey().toString(), true);
                                    }
                                }
                            });
                        }
                    }
//                    DBHelper dbHelper = new DBHelper(self);
//                    if(dbHelper.query(pairs.getKey().toString())) { // if it's the favor station
//                        ((ImageButton)mDrawerLayout.findViewById(R.id.extraInfo_favor_control)).setImageResource(R.drawable.ic_remove_favor);
//                        mDrawerLayout.findViewById(R.id.extraInfo_favor_control).setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                DBHelper helper = new DBHelper(self);
//                                int result = helper.delete(pairs.getKey().toString());
//                                if(result > 0) {
//                                    refreshFavorMarker(pairs.getKey().toString(), false);
//                                }
//                            }
//                        });
//
//                    } else {
//                        ((ImageButton)mDrawerLayout.findViewById(R.id.extraInfo_favor_control)).setImageResource(R.drawable.ic_add_favor);
//                        mDrawerLayout.findViewById(R.id.extraInfo_favor_control).setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                ContentValues values = new ContentValues();
//                                values.put(DBHelper.DB_COL_STATION_ID, pairs.getKey().toString());
//                                DBHelper helper = new DBHelper(self);
//                                long result = helper.insert(values);
//                                if (result > 0) {
//                                    refreshFavorMarker(pairs.getKey().toString(), true);
//                                }
//                            }
//                        });
//                    }

                    // TODO FIXME
                    if(isNavMode) {
                        mDrawerLayout.findViewById(R.id.extraInfo_favor_control).setVisibility(View.GONE);
                        mDrawerLayout.findViewById(R.id.extraInfo_nav_exit).setVisibility(View.VISIBLE);
                    } else {
                        mDrawerLayout.findViewById(R.id.extraInfo_favor_control).setVisibility(View.VISIBLE);
                        mDrawerLayout.findViewById(R.id.extraInfo_nav_exit).setVisibility(View.GONE);
                    }

                    final LatLng markerLatLng = marker.getPosition();
                    // setting left side extra info panel - start
                    mDrawerLayout.findViewById(R.id.extraInfo_walk).setOnClickListener(
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if(mLocationClient.isConnected() && (mLocationClient.getLastLocation() != null)) {
                                        String webConn = utils.getDirectionURL(mLocationClient.getLastLocation(),
                                                                               markerLatLng,
                                                                               Utils.travelModeWalking);
                                        if(D) { Log.d(TAG, "onClick, walk, conn: " + webConn); }
                                        setProgressBarIndeterminateVisibility(true);
                                        new DownloadJsonTask_Direction().execute(webConn, stationTitle);
                                    } else {
                                        //Toast.makeText(self, R.string.distance_hint, Toast.LENGTH_SHORT).show();
                                        showNoCurrentLocationToast();
                                    }
                                }
                            }
                    );
//                    mDrawerLayout.findViewById(R.id.extraInfo_cycling).setOnClickListener(
//                            new View.OnClickListener() {
//                                @Override
//                                public void onClick(View v) {
//                                    if(mLocationClient.isConnected() && (mLocationClient.getLastLocation() != null)) {
//                                        String webConn = utils.getDirectionURL(mLocationClient.getLastLocation(),
//                                                                               markerLatLng,
//                                                                               Utils.travelModeBicycling);
//                                        if(D) { Log.d(TAG, "onClick, bicycling, conn: " + webConn); }
//                                        setProgressBarIndeterminateVisibility(true);
//                                        new DownloadJsonTask_Direction().execute(webConn);
//                                    } else {
//                                        //Toast.makeText(self, R.string.distance_hint, Toast.LENGTH_SHORT).show();
//                                        showNoCurrentLocationToast();
//                                    }
//                                }
//                    });
                    mDrawerLayout.findViewById(R.id.extraInfo_driving).setOnClickListener(
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if(mLocationClient.isConnected() && (mLocationClient.getLastLocation() != null)) {
                                        String webConn = utils.getDirectionURL(mLocationClient.getLastLocation(),
                                                                               markerLatLng,
                                                                               Utils.travelModeDriving);
                                        if(D) { Log.d(TAG, "onClick, driving, conn: " + webConn); }
                                        setProgressBarIndeterminateVisibility(true);
                                        new DownloadJsonTask_Direction().execute(webConn, stationTitle);
                                    } else {
                                        //Toast.makeText(self, R.string.distance_hint, Toast.LENGTH_SHORT).show();
                                        showNoCurrentLocationToast();
                                    }
                                }
                            }
                    );
                    mDrawerLayout.findViewById(R.id.extraInfo_public_trans).setOnClickListener(
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if(mLocationClient.isConnected() && (mLocationClient.getLastLocation() != null)) {
                                        String webConn = utils.getDirectionURL(mLocationClient.getLastLocation(),
                                                                               markerLatLng,
                                                                               Utils.travelModeTransit);
                                        if(D) { Log.d(TAG, "onClick, transit, conn: " + webConn); }
                                        setProgressBarIndeterminateVisibility(true);
                                        new DownloadJsonTask_Direction().execute(webConn, stationTitle);
                                    } else {
                                        //Toast.makeText(self, R.string.distance_hint, Toast.LENGTH_SHORT).show();
                                        showNoCurrentLocationToast();
                                    }
                                }
                            }
                    );
                    // setting left side extra info panel - end

                    break;
                }
            }
            mDrawerLayout.findViewById(R.id.extraInfo).setVisibility(View.VISIBLE); // show info window and extra info area
            // TODO use TranslateAnimation
//            TranslateAnimation translateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, -120,
//                                                                           Animation.RELATIVE_TO_SELF, 0,
//                                                                           Animation.RELATIVE_TO_SELF, 0,
//                                                                           Animation.RELATIVE_TO_SELF, 0);
//            translateAnimation.setInterpolator(new DecelerateInterpolator());
//            translateAnimation.setStartTime(3000);
//            (mDrawerLayout.findViewById(R.id.extraInfo)).startAnimation(translateAnimation);
//            (mDrawerLayout.findViewById(R.id.extraInfo)).setVisibility(View.VISIBLE);
            // setting left extra info - end

            return mWindow;
        }

        @Override
        public View getInfoContents(Marker marker) {
            return null;
        }

        private void render(Marker marker, View view) {
            // TODO FIXME
            if(D) Log.d(TAG, "render, station_list size: " + station_list.size());

            //if(!isNavMode) {
                Iterator it = station_marker_hashmap.entrySet().iterator();
                while(it.hasNext()) {
                    Map.Entry pairs = (Map.Entry)it.next();
                    if(((Marker)pairs.getValue()).equals(marker)) { // if we get the marker that user click
                        IStation tmp = getStationById(pairs.getKey().toString());
                        ((TextView) view.findViewById(R.id.station_name)).setText(tmp.getNAME());
                        if(mLocationClient.isConnected() && (mLocationClient.getLastLocation() != null)) {
                            Location location = mLocationClient.getLastLocation();
                            LatLng currtLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            LatLng destiLocation = new LatLng(Double.valueOf(tmp.getLAT()), Double.valueOf(tmp.getLON()));
                            double dist = SphericalUtil.computeDistanceBetween(currtLocation, destiLocation);

                            ((TextView) view.findViewById(R.id.station_distance)).setText(formatNumber(dist));
                        }
                        ((TextView) view.findViewById(R.id.popup_bike)).setText(tmp.getAVAILABLE_BIKE());
                        ((TextView) view.findViewById(R.id.popup_parking)).setText(tmp.getAVAILABLE_PARKING());
                        if(D) {
                            Log.d(TAG, "You select marker, ID: " + marker.getId() +
                                    ", TITLT: " + marker.getTitle() +
                                    ", SNIPPET: " + marker.getSnippet());
                        }
                        break;
                    }
                }
            //}
        }
    }*/

    @Override
    protected void onStart() {
        super.onStart();
        mLocationClient.connect();
    }

    @Override
    protected void onStop() {
        mLocationClient.disconnect();
        super.onStop();
    }

    @Override
    public void onResume() {
        if(D) { Log.d(TAG, "onResume"); }
        super.onResume();

        setUpMapIfNeeded();
        registerReceiver(mConnReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    public void requestData(IXmlDownloader ixd) {
        if(listener.contains(ixd)) {
            getXML(false);
        } else {
            if(listener.add(ixd)) {
                getXML(false);
            }
        }
    }

    public static void removeListener(IXmlDownloader ixd) {
        boolean result = listener.remove(ixd);
        if(D) {
            if(result) Log.d(TAG, "remove a listenre, now we have: " + listener.size());
        }
    }

    public void refreshFavorMarker(String id, boolean isAdd) {
        if(isAdd) {
            for(IStation is : station_list) {
                if(is.getID().equals(id)) {
                    int num = Integer.valueOf(is.getAVAILABLE_BIKE());
                    int num_icon = marker_favor_num_array[0];
                    if(num<10) {
                        num_icon = marker_favor_num_array[num];
                    } else {
                        num_icon = marker_favor_num_array[10];
                    }
                    station_marker_hashmap.get(id).setIcon(BitmapDescriptorFactory.fromResource(num_icon));
                    //break;
                }
            }
            ((ImageButton)mDrawerLayout.findViewById(R.id.extraInfo_favor_control)).setImageResource(R.drawable.ic_remove_favor);
        } else {
            for(IStation is : station_list) {
                if(is.getID().equals(id)) {
                    int num = Integer.valueOf(is.getAVAILABLE_BIKE());
                    int num_icon = marker_num_array[0];
                    if(num<10) {
                        num_icon = marker_num_array[num];
                    } else {
                        num_icon = marker_num_array[10];
                    }
                    station_marker_hashmap.get(id).setIcon(BitmapDescriptorFactory.fromResource(num_icon));
                    //break;
                }
            }
            ((ImageButton)mDrawerLayout.findViewById(R.id.extraInfo_favor_control)).setImageResource(R.drawable.ic_add_favor);
        }
        //station_marker_hashmap.get(id).showInfoWindow();
    }

    @Override
    public void onPause() {
        if(D) { Log.d(TAG, "onPause"); }
        super.onPause();

        SharedPreferences lastLoction = getSharedPreferences("Location", 0);
        SharedPreferences.Editor editor = lastLoction.edit();
        CameraPosition position = mMap.getCameraPosition();

        editor.putFloat("zoom", position.zoom);
        editor.putString("lat", String.valueOf(position.target.latitude));
        editor.putString("lon", String.valueOf(position.target.longitude));
        editor.commit();

        try {
            unregisterReceiver(mConnReceiver);
        } catch(IllegalArgumentException iae) {
            if(D) Log.d(TAG, "never register mConnReceiver");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        //mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        //mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

//    private void setUpMapIfNeeded(XmlParser_Bike.Station ... stations) {
    private void setUpMapIfNeeded() {
        //System.out.println("stations length: " + stations.length);
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                SharedPreferences setting = getSharedPreferences("Location", 0);
                float zoom = setting.getFloat("zoom", 11.0f);
                String lat = setting.getString("lat", "22.656944");
                String lon = setting.getString("lon", "120.3575");
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(Double.valueOf(lat), Double.valueOf(lon)), zoom));

                setUpMap();
            }
        }
    }

//    private void setUpMap(XmlParser_Bike.Station ... stations) {
    private void setUpMap() {
        if(D) Log.d(TAG, "setUpMap");
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        //mMap.setInfoWindowAdapter(new MyInfoWindowAdapter());
        mMap.setOnMarkerClickListener(markerClickListener);
//        if(MAP_SOURCE_MODE == 1) {
//            mMap.setOnInfoWindowClickListener(new myInfoWindowClickListener()); // Taipei no this
//        }
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                mDrawerLayout.findViewById(R.id.extraInfo).setVisibility(View.GONE);
            }
        });
    }

    private GoogleMap.OnMarkerClickListener markerClickListener = new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
            setupExtras(marker.getTitle());
            return true;
        }
    };

    private void setupExtras(String id) {
        ((RelativeLayout)mDrawerLayout.findViewById(R.id.extraInfo)).setVisibility(View.VISIBLE);
         setupFavorControl(id);
    }

    private void setupFavorControl(final String id) {
        System.out.println("setupFavorControl, id: " + id);
        boolean isFavor = false;
        if (MAP_SOURCE_MODE == 2) { // taipei
            final DBHelper_Taipei db = new DBHelper_Taipei(self);
            if (db.query(id)) { // it's a favor
                ((ImageButton)mDrawerLayout.findViewById(R.id.extraInfo_favor_control)).setImageResource(R.drawable.ic_remove_favor);
                mDrawerLayout.findViewById(R.id.extraInfo_favor_control).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int result = db.delete(id);
                        if(result > 0) {
                            refreshFavorMarker(id, false);
                        }
                    }
                });
            } else { // it's not a favor
                ((ImageButton)mDrawerLayout.findViewById(R.id.extraInfo_favor_control)).setImageResource(R.drawable.ic_add_favor);
                mDrawerLayout.findViewById(R.id.extraInfo_favor_control).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DBHelper_Taipei dbHelper_taipei = new DBHelper_Taipei(self);
                        ContentValues values = new ContentValues();
                        values.put(DBHelper_Taipei.DB_COL_STATION_ID, id);
                        long result = dbHelper_taipei.insert(values);
                        if (result > 0) {
                            refreshFavorMarker(id, true);
                        }
                    }
                });
            }
        } else {// kaohsiung
            final DBHelper db = new DBHelper(self);
            if (db.query(id)) { // it's a favor
                System.out.println("HIHI");
            } else {
                System.out.println("YAYA");
            }
        }

        if(isFavor) {

        } else {
            ((ImageButton)mDrawerLayout.findViewById(R.id.extraInfo_favor_control)).setImageResource(R.drawable.ic_add_favor);
        }
    }



//        // setting left extra info - start
//        Iterator it = station_marker_hashmap.entrySet().iterator();
//        while(it.hasNext()) {
//            final Map.Entry pairs = (Map.Entry)it.next();
//            if(((Marker)pairs.getValue()).equals(marker)) {
//                final String stationTitle = marker.getTitle();
//
//                if(MAP_SOURCE_MODE == 2) { // taipei
//
//                    DBHelper_Taipei dbHelper_taipei = new DBHelper_Taipei(self);
//                    if(dbHelper_taipei.query(pairs.getKey().toString())) { // if it's favor one
////                        setupFavorControl_Taipei(pairs, false);
//                        ((ImageButton)mDrawerLayout.findViewById(R.id.extraInfo_favor_control)).setImageResource(R.drawable.ic_remove_favor);
//                        mDrawerLayout.findViewById(R.id.extraInfo_favor_control).setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                DBHelper_Taipei dbHelper_taipei = new DBHelper_Taipei(self);
//                                int result = dbHelper_taipei.delete(pairs.getKey().toString());
//                                if(result > 0) {
//                                    //System.out.println("refreshFavorMarker(pairs.getKey().toString(), false);");
//                                    refreshFavorMarker(pairs.getKey().toString(), false);
//                                }
//                            }
//                        });
//                    } else { // it's not favor one
////                        setupFavorControl_Taipei(pairs, true);
//                        ((ImageButton)mDrawerLayout.findViewById(R.id.extraInfo_favor_control)).setImageResource(R.drawable.ic_add_favor);
//                        mDrawerLayout.findViewById(R.id.extraInfo_favor_control).setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                DBHelper_Taipei dbHelper_taipei = new DBHelper_Taipei(self);
//                                ContentValues values = new ContentValues();
//                                values.put(DBHelper_Taipei.DB_COL_STATION_ID, pairs.getKey().toString());
//                                long result = dbHelper_taipei.insert(values);
//                                if (result > 0) {
//                                    //System.out.println("refreshFavorMarker(pairs.getKey().toString(), true);");
//                                    refreshFavorMarker(pairs.getKey().toString(), true);
//                                }
//                            }
//                        });
//                    }
//
//                } else { // kaohsiung
//                    DBHelper dbHelper = new DBHelper(self);
//                    if(dbHelper.query(pairs.getKey().toString())) {
////                        setupFavorControl(pairs, false);
//                        ((ImageButton)mDrawerLayout.findViewById(R.id.extraInfo_favor_control)).setImageResource(R.drawable.ic_remove_favor);
//                        mDrawerLayout.findViewById(R.id.extraInfo_favor_control).setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                DBHelper dbHelper = new DBHelper(self);
//                                int result = dbHelper.delete(pairs.getKey().toString());
//                                if(result > 0) {
//                                    refreshFavorMarker(pairs.getKey().toString(), false);
//                                }
//                            }
//                        });
//                    } else {
////                        setupFavorControl(pairs, true);
//                        ((ImageButton)mDrawerLayout.findViewById(R.id.extraInfo_favor_control)).setImageResource(R.drawable.ic_add_favor);
//                        mDrawerLayout.findViewById(R.id.extraInfo_favor_control).setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                ContentValues values = new ContentValues();
//                                values.put(DBHelper_Taipei.DB_COL_STATION_ID, pairs.getKey().toString());
//                                DBHelper dbHelper = new DBHelper(self);
//                                long result = dbHelper.insert(values);
//                                if (result > 0) {
//                                    refreshFavorMarker(pairs.getKey().toString(), true);
//                                }
//                            }
//                        });
//                    }
//                }
//                // TODO FIXME
//                if(isNavMode) {
//                    mDrawerLayout.findViewById(R.id.extraInfo_favor_control).setVisibility(View.GONE);
//                    mDrawerLayout.findViewById(R.id.extraInfo_nav_exit).setVisibility(View.VISIBLE);
//                } else {
//                    mDrawerLayout.findViewById(R.id.extraInfo_favor_control).setVisibility(View.VISIBLE);
//                    mDrawerLayout.findViewById(R.id.extraInfo_nav_exit).setVisibility(View.GONE);
//                }
//
//                final LatLng markerLatLng = marker.getPosition();
//                // setting left side extra info panel - start
//                // navigation -walk
//                mDrawerLayout.findViewById(R.id.extraInfo_walk).setOnClickListener(
//                        new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                if(mLocationClient.isConnected() && (mLocationClient.getLastLocation() != null)) {
//                                    String webConn = utils.getDirectionURL(mLocationClient.getLastLocation(),
//                                            markerLatLng,
//                                            Utils.travelModeWalking);
//                                    if(D) { Log.d(TAG, "onClick, walk, conn: " + webConn); }
//                                    setProgressBarIndeterminateVisibility(true);
//                                    new DownloadJsonTask_Direction().execute(webConn, stationTitle);
//                                } else {
//                                    //Toast.makeText(self, R.string.distance_hint, Toast.LENGTH_SHORT).show();
//                                    showNoCurrentLocationToast();
//                                }
//                            }
//                        }
//                );
//                // navigation -driving
//                mDrawerLayout.findViewById(R.id.extraInfo_driving).setOnClickListener(
//                        new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                if(mLocationClient.isConnected() && (mLocationClient.getLastLocation() != null)) {
//                                    String webConn = utils.getDirectionURL(mLocationClient.getLastLocation(),
//                                            markerLatLng,
//                                            Utils.travelModeDriving);
//                                    if(D) { Log.d(TAG, "onClick, driving, conn: " + webConn); }
//                                    setProgressBarIndeterminateVisibility(true);
//                                    new DownloadJsonTask_Direction().execute(webConn, stationTitle);
//                                } else {
//                                    //Toast.makeText(self, R.string.distance_hint, Toast.LENGTH_SHORT).show();
//                                    showNoCurrentLocationToast();
//                                }
//                            }
//                        }
//                );
//                // navigation -public transit
//                mDrawerLayout.findViewById(R.id.extraInfo_public_trans).setOnClickListener(
//                        new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                if(mLocationClient.isConnected() && (mLocationClient.getLastLocation() != null)) {
//                                    String webConn = utils.getDirectionURL(mLocationClient.getLastLocation(),
//                                            markerLatLng,
//                                            Utils.travelModeTransit);
//                                    if(D) { Log.d(TAG, "onClick, transit, conn: " + webConn); }
//                                    setProgressBarIndeterminateVisibility(true);
//                                    new DownloadJsonTask_Direction().execute(webConn, stationTitle);
//                                } else {
//                                    //Toast.makeText(self, R.string.distance_hint, Toast.LENGTH_SHORT).show();
//                                    showNoCurrentLocationToast();
//                                }
//                            }
//                        }
//                );
//                // setting left side extra info panel - end
//
//                break;
//            }
//        }
//        mDrawerLayout.findViewById(R.id.extraInfo).setVisibility(View.VISIBLE); // show info window and extra info area
//    }

    private void setupFavorControl(final Map.Entry pairs, boolean isAddMode) { // Kaohsiung
        if(isAddMode) { // Add
            ((ImageButton)mDrawerLayout.findViewById(R.id.extraInfo_favor_control)).setImageResource(R.drawable.ic_add_favor);
            mDrawerLayout.findViewById(R.id.extraInfo_favor_control).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ContentValues values = new ContentValues();
                    values.put(DBHelper_Taipei.DB_COL_STATION_ID, pairs.getKey().toString());
                    DBHelper dbHelper = new DBHelper(self);
                    long result = dbHelper.insert(values);
                    if (result > 0) {
                        refreshFavorMarker(pairs.getKey().toString(), true);
                        setupFavorControl(pairs, false);
                    }
                }
            });
        } else { // Remove
            ((ImageButton)mDrawerLayout.findViewById(R.id.extraInfo_favor_control)).setImageResource(R.drawable.ic_remove_favor);
            mDrawerLayout.findViewById(R.id.extraInfo_favor_control).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DBHelper dbHelper = new DBHelper(self);
                    int result = dbHelper.delete(pairs.getKey().toString());
                    if(result > 0) {
                        refreshFavorMarker(pairs.getKey().toString(), false);
                        setupFavorControl(pairs, true);
                    }
                }
            });
        }
    }

    private void setupFavorControl_Taipei(final Map.Entry pairs, boolean isAddMode) { // Taipei
        if(isAddMode) { // Add
            mDrawerLayout.findViewById(R.id.extraInfo_favor_control).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ContentValues values = new ContentValues();
                    values.put(DBHelper_Taipei.DB_COL_STATION_ID, pairs.getKey().toString());
                    DBHelper dbHelper = new DBHelper(self);
                    long result = dbHelper.insert(values);
                    if (result > 0) {
                        refreshFavorMarker(pairs.getKey().toString(), true);
                        setupFavorControl_Taipei(pairs, false);
                    }
                }
            });
        } else { // Remove
            mDrawerLayout.findViewById(R.id.extraInfo_favor_control).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DBHelper_Taipei dbHelper_taipei = new DBHelper_Taipei(self);
                    int result = dbHelper_taipei.delete(pairs.getKey().toString());
                    if(result > 0) {
                        //System.out.println("refreshFavorMarker(pairs.getKey().toString(), false);");
                        refreshFavorMarker(pairs.getKey().toString(), false);
                        setupFavorControl_Taipei(pairs, true);
                    }
                }
            });
        }
    }

    private synchronized void getXML(boolean isActionBarShowing) {
        if(D) Log.d(TAG, "getXML");

        if(isActionBarShowing) { setProgressBarIndeterminateVisibility(true); }

        if(MAP_SOURCE_MODE == 2) {
            new DownloadXmlTask().execute("TAIPEI");
        } else {
            new DownloadXmlTask().execute("KAOHSIUNG");
        }
    }

    // to get XML data from Kaohsiung Government Open Data - Start
    private class DownloadXmlTask extends AsyncTask<String, Void, HashMap<String, IStation>> {
        @Override
        protected HashMap<String, IStation> doInBackground(String... inputs) {
            if(D) Log.d(TAG, "DownloadXmlTask - doInBackground");
            try {
                if(inputs[0].equals("TAIPEI")) {
                    JsonParser_Bike_Taipei parser = new JsonParser_Bike_Taipei();
                    return parser.getStationHashMap();
                } else {
                    XmlParser_Bike parser = new XmlParser_Bike();
                    return parser.getStationHashMap();
                }

            } catch(Exception e) {
                if(D) Log.d(TAG, e.toString());
                return null;
            }
        }

        @Override
        protected void onPostExecute(HashMap<String, IStation> stationList) {
            if(station_list != null) {
                if(D) Log.d(TAG, "stationList length: " + stationList.size() + "station_list size:" + station_list.size());
            }
            //station_list = stationList;
            station_hashmap = stationList;
            if(D) Log.d(TAG, "stationList length: " + stationList.size() + "station_list size:" + station_list.size());
            if((handler != null) && (stationList.size() != 0)) {
                Message message = new Message();
                message.what = MSG_DOWNLOAD_OK;
                handler.sendMessage(message);
            }

            for(IXmlDownloader ixd: listener) {
                if(station_list != null) {
                    if(D) Log.d(TAG, "DownloadXmlTask - onPostExecute, OK");
                    ixd.downloadOK(true);
                } else {
                    if(D) Log.d(TAG, "DownloadXmlTask - onPostExecute, not OK");
                    ixd.downloadOK(false);
                }
            }
        }
    }
    // to get XML data from Kaohsiung Government Open Data - end

    // to get json data from google map direction api -start
    private class DownloadJsonTask_Direction extends AsyncTask<String, Void, JsonParser_Direction> {
        String stationTitle = "";

        @Override
        protected JsonParser_Direction doInBackground(String... inputs) {
            if(D) Log.d(TAG, "DownloadJsonTask_Direction - doInBackground");
            JsonParser_Direction parser = new JsonParser_Direction(inputs[0]);
            stationTitle = inputs[1];
            return parser;
        }

        @Override
        protected void onPostExecute(JsonParser_Direction parser) {
            //super.onPostExecute(steps);
            if(D) { Log.d(TAG, "DownloadJsonTask_Direction, onPostExecute"); }
            List<JsonParser_Direction.Steps> steps = parser.getAllSteps();
            //List<LatLng> allPoints = new ArrayList<LatLng>();
            PolylineOptions polylineOptions = null;
            if(D) { Log.d(TAG, "DownloadJsonTask_Direction, onPostExecute, steps size:  " + steps.size()); }
            if(!parser.getErrorStatus().equals("")) {
                Toast.makeText(self, R.string.nav_no_route, Toast.LENGTH_SHORT).show();
            }
            if(steps.size() > 0) {
                mMap.clear();
                String travel_mode = "";
                for(JsonParser_Direction.Steps s : steps) {
                    polylineOptions = new PolylineOptions();
                    List<LatLng> point = s.getDecodePoints();
                    if(false) {
                        for(LatLng ll : point) {
                            Log.d(TAG, "DownloadJsonTask_Direction - onPostExecute, point: " + ll.toString());
                        }
                    }
                    polylineOptions.addAll(point);
                    polylineOptions.width(5);
                    polylineOptions.color(Color.BLUE);

                    mMap.addPolyline(polylineOptions);
                    if(!s.travel_mode.equals(travel_mode)) {
                        String title = Jsoup.parse(s.html_instructions).text();
                        StringBuilder snippet = new StringBuilder();
                        travel_mode = s.travel_mode;
                        LatLng nowLatLng = new LatLng(Double.parseDouble(s.start_location.get("lat")),
                                                      Double.parseDouble(s.start_location.get("lng")));
                        int imgRes = R.drawable.ic_marker_nav_default;
                        if(s.travel_mode.equals(Utils.travelModeWalkingUpper)) {
                            imgRes = R.drawable.ic_marker_nav_walking;
                        } else
                        if(s.travel_mode.equals(Utils.travelModeDrivingUpper)) {
                            imgRes = R.drawable.ic_marker_nav_driving;
                        } else
                        if(s.travel_mode.equals(Utils.travelModeTransitUpper)) {
                            imgRes = R.drawable.ic_marker_nav_transit;
                            snippet.append(getResources().getString(R.string.nav_transit_take));
                            snippet.append(s.getTransitDetails().getTransitShortName());
                            snippet.append("\n");
                            snippet.append(getResources().getString(R.string.nav_transit_from));
                            snippet.append(s.getTransitDetails().getDepartureStop());
                            snippet.append("\n");
                            snippet.append(getResources().getString(R.string.nav_transit_to));
                            snippet.append(s.getTransitDetails().getArrivalStop());
                        }
                        mMap.addMarker(
                                new MarkerOptions().position(nowLatLng)
                                        .title(title)
                                        .snippet(snippet.toString())
                                        .icon(BitmapDescriptorFactory.fromResource(imgRes))
                                        .draggable(false)
                        );
                    }

                }
                String endLat = steps.get(steps.size()-1).end_location.get("lat");
                String endLng = steps.get(steps.size()-1).end_location.get("lng");
                StringBuilder snippet = new StringBuilder(parser.getAllDistance());
                snippet.append(System.getProperty("line.separator"));
                snippet.append(parser.getAllTime());
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(Double.parseDouble(endLat), Double.parseDouble(endLng)))
                        .title(stationTitle)
                        .snippet(snippet.toString())
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_nav_finish))
                        .draggable(false));
                setIsNavMode(true);
                mMap.setInfoWindowAdapter(new NavInfoWindowAdapter());
            }

            setProgressBarIndeterminateVisibility(false);
        }
    }
    // to get json data from google map direction api -end

    private class NavInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        private final View mWindow;
        NavInfoWindowAdapter() {
            mWindow = getLayoutInflater().inflate(R.layout.map_nav_info_window, null);
        }
        @Override
        public View getInfoWindow(Marker marker) {
            render(marker, mWindow);
            return mWindow;
        }
        @Override
        public View getInfoContents(Marker marker) {
            return null;
        }
        private void render(Marker marker, View view) {
            ((TextView) view.findViewById(R.id.nav_station_name)).setText(marker.getTitle());
            ((TextView) view.findViewById(R.id.nav_station_direction)).setText(marker.getSnippet());
        }
    }

    // to set isNavMode -start
    // TODO setupNavMode
    private void setIsNavMode(boolean yes_no){
        isNavMode = yes_no;

        if(!yes_no) { // not nav
            //mDrawerLayout.findViewById(R.id.extraInfo_nav_exit).setVisibility(View.GONE);
            //mDrawerLayout.findViewById(R.id.extraInfo_favor_control).setVisibility(View.VISIBLE);
            setUpMap();
            if((handler != null) && (station_list.size() != 0)) {
                Message message = new Message();
                message.what = MSG_DOWNLOAD_OK;
                handler.sendMessage(message);
            } else {
                getXML(true);
            }
            mDrawerLayout.findViewById(R.id.extraInfo).setVisibility(View.GONE);
        } else { // in nav mode
            mDrawerLayout.findViewById(R.id.extraInfo_nav_exit).setVisibility(View.VISIBLE); //nav mode, show 'exit'
            mDrawerLayout.findViewById(R.id.extraInfo_favor_control).setVisibility(View.GONE); // nav mode, hide 'favor'
            mDrawerLayout.findViewById(R.id.extraInfo).setVisibility(View.VISIBLE);
            //mDrawerLayout.findViewById(R.id.extraInfo_extra).setVisibility(View.GONE);
            //mMap.setInfoWindowAdapter(null);
            mMap.setOnMapClickListener(null);
        }
    }
    // to set isNavMode -end

    //private static LatLng preLatLng = null;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        myMenu = menu;

        //final com.actionbarsherlock.view.MenuItem item_open_list = menu.add(R.string.list_view);
        MenuItem item_open_list =
                menu.add(0, 0, Menu.FIRST, R.string.list_view);
        item_open_list.setIcon(R.drawable.ic_action_list);
        item_open_list.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        MenuItem item_refresh =
                menu.add(0, 1, Menu.FIRST+1, R.string.refresh);
        item_refresh.setIcon(R.drawable.ic_action_refresh);
        item_refresh.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        final MenuItem item_favor_list =
                menu.add(0, 2, Menu.FIRST+2, R.string.favor_list_view);
        item_favor_list.setIcon(R.drawable.ic_favor_list_v2);
        item_favor_list.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);


        item_open_list.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (mDrawerLayout.isDrawerOpen(Gravity.LEFT)) { // it itself is opening
                    mDrawerLayout.closeDrawer(Gravity.LEFT);
                    //mDrawerList.setAdapter(null);
                } else { // now will start to opent itself
                    if (mLocationClient.isConnected() && (mLocationClient.getLastLocation() != null)) {
                        utils.setLocation(mLocationClient.getLastLocation());
                    }

                    if (mDrawerLayout.isDrawerOpen(Gravity.RIGHT)) { // but if the right is opening, close it.
                        mDrawerLayout.closeDrawer(Gravity.RIGHT);
//                        mDrawerListRight.setAdapter(null);
                    }

                    // left list will be set in Handler
                    mDrawerLayout.openDrawer(Gravity.LEFT);
                }
                return true;
            }
        });

        item_refresh.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                mDrawerList.setAdapter(null);
                mDrawerListRight.setAdapter(null);

                if(mLocationClient.isConnected() && (mLocationClient.getLastLocation() != null)) {
                    utils.setLocation(mLocationClient.getLastLocation());
                }

                mDrawerLayout.findViewById(R.id.extraInfo).setVisibility(View.GONE); // click refresh, hide extra info
                mDrawerLayout.closeDrawers();
                //item_open_list.setIcon(R.drawable.ic_action_list);
                //item_favor_list.setIcon(R.drawable.ic_favor_list_v2);


                getXML(true);

                setIsNavMode(false);
                return true;
            }
        });

        //final com.actionbarsherlock.view.MenuItem item_favor_list = menu.add(R.string.favor_list_view);
        item_favor_list.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(mDrawerLayout.isDrawerOpen(Gravity.RIGHT)) {// if itself is opening, close it!
                    mDrawerLayout.closeDrawer(Gravity.RIGHT);
                    if(D) { Log.d(TAG, "item_favor_list, onClick, close self"); }
                } else { // will open itself now
                    if(mLocationClient.isConnected() && (mLocationClient.getLastLocation() != null)) {
                        utils.setLocation(mLocationClient.getLastLocation());
                    }

                    if(mDrawerLayout.isDrawerOpen(Gravity.LEFT)) { // if LEFT is opening, close it!
                        mDrawerLayout.closeDrawer(Gravity.LEFT);
                        if(D) { Log.d(TAG, "item_favor_list, onClick, close left"); }
                    }

                    Cursor cursor;
                    if(MAP_SOURCE_MODE == 2) { // taipei
                        DBHelper_Taipei dbHelper = new DBHelper_Taipei(self);
                        cursor = dbHelper.query();
                    } else {                   // kaohsiung
                        DBHelper dbHelper = new DBHelper(self);
                        cursor = dbHelper.query();
                    }
                    if(cursor.getCount() > 0 && station_hashmap != null) {
                        int index = cursor.getColumnIndexOrThrow(DBHelper.DB_COL_STATION_ID);
                        List<IStation> favor_stations = new ArrayList<IStation>();
                        cursor.moveToFirst();
                        do {
                            IStation tmp = station_hashmap.get(cursor.getString(index));
                            if(tmp != null) {
                                favor_stations.add(tmp);
                            }
                        } while(cursor.moveToNext());

                        mDrawerListRight.setAdapter(new BikeStationMapListAdapter(getLayoutInflater(), favor_stations));
                        mDrawerListRight.setOnItemClickListener(new DrawerItemClickListener());
                        if(D) { Log.d(TAG, "item_favor_list, onClick, should open drawer"); }
                        mDrawerLayout.openDrawer(Gravity.RIGHT);
                        //item_favor_list.setIcon(R.drawable.ic_close_menu_right);
                    } else {
                        Toast.makeText(self, R.string.no_favor_hint, Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
//        if (mDrawerToggle.onOptionsItemSelected(getMenuItem(item))) {
//            return true;
//        }

        // Handle action buttons
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private MenuItem getMenuItem(final MenuItem item) {
        return new MenuItem() {
            @Override
            public int getItemId() {
                return item.getItemId();
            }

            public boolean isEnabled() {
                return true;
            }

            @Override
            public boolean collapseActionView() {
                return false;
            }

            @Override
            public boolean expandActionView() {
                return false;
            }

            @Override
            public ActionProvider getActionProvider() {
                return null;
            }

            @Override
            public View getActionView() {
                return null;
            }

            @Override
            public char getAlphabeticShortcut() {
                return 0;
            }

            @Override
            public int getGroupId() {
                return 0;
            }

            @Override
            public Drawable getIcon() {
                return null;
            }

            @Override
            public Intent getIntent() {
                return null;
            }

            @Override
            public ContextMenuInfo getMenuInfo() {
                return null;
            }

            @Override
            public char getNumericShortcut() {
                return 0;
            }

            @Override
            public int getOrder() {
                return 0;
            }

            @Override
            public SubMenu getSubMenu() {
                return null;
            }

            @Override
            public CharSequence getTitle() {
                return null;
            }

            @Override
            public CharSequence getTitleCondensed() {
                return null;
            }

            @Override
            public boolean hasSubMenu() {
                return false;
            }

            @Override
            public boolean isActionViewExpanded() {
                return false;
            }

            @Override
            public boolean isCheckable() {
                return false;
            }

            @Override
            public boolean isChecked() {
                return false;
            }

            @Override
            public boolean isVisible() {
                return false;
            }

            @Override
            public MenuItem setActionProvider(ActionProvider actionProvider) {
                return null;
            }

            @Override
            public MenuItem setActionView(View view) {
                return null;
            }

            @Override
            public MenuItem setActionView(int resId) {
                return null;
            }

            @Override
            public MenuItem setAlphabeticShortcut(char alphaChar) {
                return null;
            }

            @Override
            public MenuItem setCheckable(boolean checkable) {
                return null;
            }

            @Override
            public MenuItem setChecked(boolean checked) {
                return null;
            }

            @Override
            public MenuItem setEnabled(boolean enabled) {
                return null;
            }

            @Override
            public MenuItem setIcon(Drawable icon) {
                return null;
            }

            @Override
            public MenuItem setIcon(int iconRes) {
                return null;
            }

            @Override
            public MenuItem setIntent(Intent intent) {
                return null;
            }

            @Override
            public MenuItem setNumericShortcut(char numericChar) {
                return null;
            }

            @Override
            public MenuItem setOnActionExpandListener(OnActionExpandListener listener) {
                return null;
            }

            @Override
            public MenuItem setOnMenuItemClickListener(OnMenuItemClickListener menuItemClickListener) {
                return null;
            }

            @Override
            public MenuItem setShortcut(char numericChar, char alphaChar) {
                return null;
            }

            @Override
            public void setShowAsAction(int actionEnum) {

            }

            @Override
            public MenuItem setShowAsActionFlags(int actionEnum) {
                return null;
            }

            @Override
            public MenuItem setTitle(CharSequence title) {
                return null;
            }

            @Override
            public MenuItem setTitle(int title) {
                return null;
            }

            @Override
            public MenuItem setTitleCondensed(CharSequence title) {
                return null;
            }

            @Override
            public MenuItem setVisible(boolean visible) {
                return null;
            }
        };
    }


    // get current location - start
    @Override
    public void onConnected(Bundle bundle) {
        if(D) Log.d(TAG, "onConnected! ");
        Location mCurrentLocation = mLocationClient.getLastLocation();
        //if(D) { Log.d(TAG, "onConnected, " + "Latitude: " + mCurrentLocation.getLatitude() +
        //        "Longitude: " + mCurrentLocation.getLongitude()); }
    }

    @Override
    public void onDisconnected() {
        if(D) Log.d(TAG, "onDisconnected! ");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                if(D) Log.e(TAG, e.getMessage());
                //e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            if(D) Log.e(TAG, connectionResult.getErrorCode() + "");
            //showErrorDialog(connectionResult.getErrorCode());
        }
    }
    // get current location - end

    // use marker clusterer - start
    /*public class MyItem implements ClusterItem {
        private final LatLng mPosition;

        public MyItem(double lat, double lng) {
            mPosition = new LatLng(lat, lng);
        }

        @Override
        public LatLng getPosition() {
            return mPosition;
        }
    }*/
    // usr marker clusterer - end

    // count distance - start
    private String formatNumber(double distance) {
        //String unit = "m";
        String unit = getResources().getString(R.string.meter);
        if (distance < 1) {
            distance *= 1000;
            //unit = "mm";
            unit = getResources().getString(R.string.millimeter);
        } else if (distance > 1000) {
            distance /= 1000;
            //unit = "km";
            unit = getResources().getString(R.string.kilometer);
        }

        return String.format("%4.3f%s", distance, unit);
    }
    // count distance - end

    // no current location alert - start
    private void showNoCurrentLocationToast() {
        Toast.makeText(self, getString(R.string.distance_hint), Toast.LENGTH_SHORT).show();
    }
    // no current location alert - end
}