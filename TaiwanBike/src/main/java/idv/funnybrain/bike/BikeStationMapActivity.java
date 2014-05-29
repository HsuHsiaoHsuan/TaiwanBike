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
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.Html;
import android.util.Log;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.*;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import idv.funnybrain.bike.data.*;
import idv.funnybrain.bike.database.DBHelper;
import idv.funnybrain.bike.database.DBHelper_Taipei;
import idv.funnybrain.bike.database.IDBHelper;

import java.util.*;

/**
 * Created by Freeman on 2014/2/18.
 */
public class BikeStationMapActivity extends SherlockFragmentActivity implements GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    // ---- constants START ----
    private static final boolean D = false;
    private static final String TAG = "BikeStationMapActivity";
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private final Utils utils = new Utils();
    static final int[] marker_num_array = {
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

    static final int[] marker_favor_num_array = {
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
    // ---- constants END ----

    // ---- local variable START ----
    private static int MAP_SOURCE_MODE = 1; // 0 for nothing, 1 for Kaohsiung, 2 for Taipei, 3 for both.
    private static HashMap<String, Marker> station_marker_hashmap = new HashMap<String, Marker>(); // for all(maybe Kaohsiung + Taipei)
    //static List<IStation> station_list = new ArrayList<IStation>();
    static HashMap<String, IStation> station_hashmap = new HashMap<String, IStation>();
    private static List<IXmlDownloader> listener = new ArrayList<IXmlDownloader>();

    private Handler handler;
    private final int MSG_DOWNLOAD_OK = 999;
    private GoogleMap mMap;
    private ClusterManager<MyItem> mClusterManager;
    private LocationClient mLocationClient;
    // -- for DrawerLayout START --
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ListView mDrawerListRight;
    private ActionBarDrawerToggle mDrawerToggle;
    // -- for DrawerLayout END --
    private BroadcastReceiver mConnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (utils.checkNetworkAvailable(BikeStationMapActivity.this)) { // available
                if(D) Log.d(TAG, "onReceive, networkAvailable check ok");
                // FIXME
                // TODO should check the status, than decide it's true of false
                if(!isNavMode) { getXML(false); }
            } else { // not available
                Toast.makeText(BikeStationMapActivity.this, R.string.check_netowrk, Toast.LENGTH_LONG).show();
            }
        }
    };
    private SearchView.OnQueryTextListener queryTextListener;
    private GoogleMap.OnMarkerClickListener markerClickListener = new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            LatLng position = marker.getPosition();
            //mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
            mMap.animateCamera(CameraUpdateFactory.newLatLng(position));
            if(!isNavMode) {
                setupExtras(marker.getTitle());
                return true;
            } else {
                return false;
            }
            //return true;// if return true, it wont show marker info window.
        }
    };

    private BikeStationMapActivity self;
    private boolean isNavMode = false;
    private Menu myMenu;
    private IDBHelper dbHelper;
    private IParser parser;
    private String selected_station_id = "";
    private boolean isMapCleared = false;
    // ---- local variable END ----

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_bikemap);

        MAP_SOURCE_MODE = (new Utils()).getMapSourceMode(this);

        Intent intent = getIntent();
        int tmp = intent.getIntExtra("MODE", 9);
        if(tmp != 9) {
            MAP_SOURCE_MODE = tmp;
        } else {
            MAP_SOURCE_MODE = (new Utils()).getMapSourceMode(this);
        }

        if(MAP_SOURCE_MODE == 1) {
            dbHelper = new DBHelper(this);
            parser = new XmlParser_Bike();
        } else {
            dbHelper = new DBHelper_Taipei(this);
            parser = new JsonParser_Bike_Taipei();
        }

        self = this;

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

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



        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case MSG_DOWNLOAD_OK:
                        if(D) Log.d(TAG, "handlMessage, I get message!");
                        Toast.makeText(BikeStationMapActivity.this, R.string.update_ok, Toast.LENGTH_LONG).show();
                        mMap.clear();
                        station_marker_hashmap.clear();

                        if(D) Log.d(TAG, "station_list length: " + station_hashmap.size());
                        Iterator<Map.Entry<String, IStation>> iterator = station_hashmap.entrySet().iterator();
                        while(iterator.hasNext()) {
                            Map.Entry<String, IStation> entry = iterator.next();
                            String entry_idx = entry.getKey();
                            IStation entry_station = entry.getValue();
                            boolean isFavor = false;
                            if(dbHelper.query(entry_station.getID())) { isFavor = true; }

                            if(station_marker_hashmap.get(entry_idx) != null) {
                                refreshFavorMarker(entry_idx, isFavor);
                                //if(D) Log.d(TAG, "---->1");
                            } else {
                                //if(D) Log.d(TAG, "---->2");
                                if(entry_station.getLAT().equals("") || entry_station.getLON().equals("")) { continue; }
                                LatLng tmpLatLng = new LatLng(Double.valueOf(entry_station.getLAT().replaceAll("\\p{C}", "")),
                                        Double.valueOf(entry_station.getLON().replaceAll("\\p{C}", "")));
                                int icon = getMarkerIcon(Integer.valueOf(entry_station.getAVAILABLE_BIKE()), isFavor);
                                station_marker_hashmap.put(
                                        entry_idx, mMap.addMarker(
                                                new MarkerOptions().position(tmpLatLng)
                                                        .title(entry.getValue().getID())
                                                        .icon(BitmapDescriptorFactory.fromResource(icon))
                                                        .draggable(false)));
                            }
                        }

                        if(mLocationClient.isConnected() && (mLocationClient.getLastLocation() != null)) { // get current location
                            utils.setLocation(mLocationClient.getLastLocation());
                        }
                        // set new station_list to left list
                        mDrawerList.setAdapter(new BikeStationMapListAdapter(getLayoutInflater(), new ArrayList<IStation>(station_hashmap.values())));
                        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

                        //setUpMap();
                        mMap.setOnMarkerClickListener(markerClickListener);

                        setProgressBarIndeterminateVisibility(false);
                        break;
                }
            }
        };
        mDrawerLayout.findViewById(R.id.extraInfo_nav_exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setIsNavMode(false);
                mMap.clear();
                station_marker_hashmap.clear();
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

            LatLng focusLocation = new LatLng(Double.valueOf(station_hashmap.get(selected_station_id).getLAT().replaceAll("\\p{C}", "")),
                                              Double.valueOf(station_hashmap.get(selected_station_id).getLON().replaceAll("\\p{C}", "")));
            if(D) {
                Log.d(TAG, "selected id: " + ((TextView) view.findViewById(R.id.site_id)).getText() +
                           ", selected text: " + ((TextView) view.findViewById(R.id.title)).getText());
            }
            //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(focusLocation, 18.0f));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(focusLocation, 15.0f));
            mDrawerLayout.findViewById(R.id.extraInfo).setVisibility(View.VISIBLE);

            setupFavorControl(selected_station_id);

            if(D) Log.d(TAG, "station_marker_hashmap legth: " + station_marker_hashmap.size());
        }
    }

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

        if(!isNavMode) { setUpMapIfNeeded(); }
        registerReceiver(mConnReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        // ---- setup SearchView START ----
        queryTextListener = new SearchView.OnQueryTextListener() {
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

                // TODO
                // TODO
                // TODO
                // TODO
                // TODO
                // TODO
                // TODO
                // TODO
                // TODO
                // TODO
                // TODO
                // TODO
                // TODO
                // TODO
                // TODO
                // TODO
                // TODO
                //仍然有問題，開著程式，然後再開別的程式覆蓋過去，別的程式按BACK關掉，#218會當掉!
                // 還有9版本沒有SearchView問題也待解決！

                return true;
            }
        };
        SearchView searchView = (SearchView) findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(queryTextListener);
        // ---- setup SearchView END ----
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
        //if(D) { Log.d(TAG, "refreshFavorMarker, id: " + id); }
        IStation station = station_hashmap.get(id);
        int num = Integer.valueOf(station.getAVAILABLE_BIKE());
        Marker selected = station_marker_hashmap.get(id);
//        if(D) { Log.d(TAG, "refreshFavorMarker, title: " + selected.getTitle() +
//                                             ", position: " + selected.getPosition()); }
        if(isAdd) {
            int num_icon = marker_favor_num_array[0];
            if(num<10) {
                num_icon = marker_favor_num_array[num];
            } else {
                num_icon = marker_favor_num_array[10];
            }
            selected.setIcon(BitmapDescriptorFactory.fromResource(num_icon));
        } else {
            int num_icon = marker_num_array[0];
            if(num<10) {
                num_icon = marker_num_array[num];
            } else {
                num_icon = marker_num_array[10];
            }
            selected.setIcon(BitmapDescriptorFactory.fromResource(num_icon));
        }
        //station_marker_hashmap.get(id).showInfoWindow();
    }

    @Override
    public void onPause() {
        if(D) { Log.d(TAG, "onPause"); }
        super.onPause();

        (new Utils()).setMapPreSourceMode(this, MAP_SOURCE_MODE);

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
        if(dbHelper != null) {
            dbHelper.close();
        }
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
                //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(Double.valueOf(lat), Double.valueOf(lon)), zoom));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(Double.valueOf(lat), Double.valueOf(lon)), zoom));

                setUpMap();
            }
        }
    }

    private void setUpMap() {
        if(D) Log.d(TAG, "setUpMap");
        int pre = (new Utils()).getMapPreSourceMode(this);
        if(station_hashmap.size() >= 0 && (pre == MAP_SOURCE_MODE)) {
            station_marker_hashmap.clear();
            Iterator<Map.Entry<String, IStation>> iterator = station_hashmap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, IStation> entry = iterator.next();
                String entry_idx = entry.getKey();
                IStation entry_station = entry.getValue();
                boolean isFavor = false;
                if (dbHelper.query(entry_station.getID())) {
                    isFavor = true;
                }

                    //if (D) Log.d(TAG, "---->3");
                    if(entry_station.getLAT().equals("") || entry_station.getLON().equals("")) { continue; }
                    LatLng tmpLatLng = new LatLng(Double.valueOf(entry_station.getLAT().replaceAll("\\p{C}", "")),
                            Double.valueOf(entry_station.getLON().replaceAll("\\p{C}", "")));
                    int icon = getMarkerIcon(Integer.valueOf(entry_station.getAVAILABLE_BIKE()), isFavor);
                    station_marker_hashmap.put(
                            entry_idx, mMap.addMarker(
                                    new MarkerOptions().position(tmpLatLng)
                                            .title(entry.getValue().getID())
                                            .icon(BitmapDescriptorFactory.fromResource(icon))
                                            .draggable(false)
                            )
                    );
            }
        }


        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        //mMap.setInfoWindowAdapter(new MyInfoWindowAdapter());

        mMap.setOnMarkerClickListener(markerClickListener);
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                mDrawerLayout.findViewById(R.id.extraInfo).setVisibility(View.GONE);
                if(!selected_station_id.equals("")) {
                    refreshFavorMarker(selected_station_id, dbHelper.query(selected_station_id));
                }
            }
        });
    }

    private void setupExtras(String id) {
        ((RelativeLayout)mDrawerLayout.findViewById(R.id.extraInfo)).setVisibility(View.VISIBLE);
        setupFavorControl(id);
    }

    private void setupFavorControl(final String id) {
        IStation station = station_hashmap.get(id);
        ((TextView) mDrawerLayout.findViewById(R.id.extraInfo_name)).setText(station.getNAME());
        ((TextView) mDrawerLayout.findViewById(R.id.extraInfo_nums_bicycle)).setText(station.getAVAILABLE_BIKE());
        ((TextView) mDrawerLayout.findViewById(R.id.extraInfo_nums_parking)).setText(station.getAVAILABLE_PARKING());

        boolean isFavor = false;
        ImageButton ib_favor = (ImageButton) mDrawerLayout.findViewById(R.id.extraInfo_favor_control);
        isFavor = dbHelper.query(id);

        if(!selected_station_id.equals("")) {
            refreshFavorMarker(selected_station_id, dbHelper.query(selected_station_id));
        }
        selected_station_id = id;

        if(isFavor) {
            ib_favor.setImageResource(R.drawable.ic_remove_favor);
            ib_favor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int result = dbHelper.delete(id);
                    if(result > 0) {
                        refreshFavorMarker(id, false);
                        setupFavorControl(id);
                    }
                }
            });
                station_marker_hashmap.get(id).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_favor));
        } else {
            ib_favor.setImageResource(R.drawable.ic_add_favor);
            ib_favor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ContentValues values = new ContentValues();
                    values.put(IDBHelper.DB_COL_STATION_ID, id);
                    long result = dbHelper.insert(values);
                    if(result > 0) {
                        refreshFavorMarker(id, true);
                        setupFavorControl(id);
                    }
                }
            });
            station_marker_hashmap.get(id).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_bike));
        }
        setupNavControl(id);
    }

    private void setupNavControl(String id) {
        Marker tmpMarker = station_marker_hashmap.get(id);
        final LatLng markerLatLng = tmpMarker.getPosition();
        final String stationTitle = station_hashmap.get(id).getNAME();
        mDrawerLayout.findViewById(R.id.extraInfo_walk).setOnClickListener(new View.OnClickListener() {
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
                    showNoCurrentLocationToast();
                }
            }
        });
        // bicycle navigation is not available in Taiwan
//        mDrawerLayout.findViewById(R.id.extraInfo_cycling).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if(mLocationClient.isConnected() && (mLocationClient.getLastLocation() != null)) {
//                    String webConn = utils.getDirectionURL(mLocationClient.getLastLocation(),
//                            markerLatLng,
//                            Utils.travelModeCycling);
//                    if(D) { Log.d(TAG, "onClick, walk, conn: " + webConn); }
//                    setProgressBarIndeterminateVisibility(true);
//                    new DownloadJsonTask_Direction().execute(webConn, stationTitle);
//                } else {
//                    showNoCurrentLocationToast();
//                }
//            }
//        });
        mDrawerLayout.findViewById(R.id.extraInfo_driving).setOnClickListener(new View.OnClickListener() {
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
        });
        mDrawerLayout.findViewById(R.id.extraInfo_public_trans).setOnClickListener(new View.OnClickListener() {
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
        });
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
                parser.downloadData();
                return parser.getStationHashMap();
            } catch(Exception e) {
                if(D) Log.d(TAG, e.toString());
                return null;
            }
        }

        @Override
        protected void onPostExecute(HashMap<String, IStation> stationList) {
            station_hashmap = stationList;
            if((handler != null) && (stationList.size() != 0)) {
                Message message = new Message();
                message.what = MSG_DOWNLOAD_OK;
                handler.sendMessage(message);
            }

            for(IXmlDownloader ixd: listener) {
                if(station_hashmap != null) {
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
            mMap.clear();
            station_marker_hashmap.clear();
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

                if(D) {
                    for (JsonParser_Direction.Steps s : steps) {
                        Log.d(TAG, "HTML_INSTRUCTION: " + s.getHtml_instructions());
                    }
                }
                //String travel_mode = "";
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
                    //if(D) { Log.d(TAG, "last travel_mode: " + travel_mode + ", new travel_mode: " + s.travel_mode); }
                    //if(!s.travel_mode.equals(travel_mode)) {
                        //String title = Jsoup.parse(s.getHtml_instructions()).text();
                        StringBuilder snippet = new StringBuilder();
                        //travel_mode = s.travel_mode;
                        LatLng nowLatLng = new LatLng(Double.parseDouble(s.getStart_location().get("lat")),
                                                      Double.parseDouble(s.getStart_location().get("lng")));
                    if(D) { Log.d(TAG, "nowLatLng: " + nowLatLng); }
                        int imgRes = R.drawable.ic_marker_nav_default;
                        if(s.getTravel_mode().equals(Utils.travelModeWalkingUpper)) {
                            imgRes = R.drawable.ic_marker_nav_walking;
                            if(D) { Log.d(TAG, "It's WALKING"); }
                        } else
                        if(s.getTravel_mode().equals(Utils.travelModeDrivingUpper)) {
                            imgRes = R.drawable.ic_marker_nav_driving;
                            if(D) { Log.d(TAG, "It's DRIVING"); }
                        } else
                        if(s.getTravel_mode().equals(Utils.travelModeTransitUpper)) {
                            if(D) { Log.d(TAG, "It's TRANSIT"); }
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
                        if(D) { Log.d(TAG, "snippet: " + snippet); }
                        mMap.addMarker(
                                new MarkerOptions().position(nowLatLng)
                                        .title(Html.fromHtml(s.getHtml_instructions()).toString())
                                        .snippet(snippet.toString())
                                        .icon(BitmapDescriptorFactory.fromResource(imgRes))
                                        .draggable(false)
                        );
                    //}

                }
                String endLat = steps.get(steps.size()-1).getEnd_location().get("lat");
                String endLng = steps.get(steps.size()-1).getEnd_location().get("lng");
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
            if((handler != null) && (station_hashmap.size() != 0)) {
                Message message = new Message();
                message.what = MSG_DOWNLOAD_OK;
                handler.sendMessage(message);
            } else {
                getXML(true);
            }
            mDrawerLayout.findViewById(R.id.extraInfo).setVisibility(View.GONE);
            mDrawerLayout.findViewById(R.id.extraInfo_nav_exit).setVisibility(View.GONE); //nav mode, show 'exit'
            mDrawerLayout.findViewById(R.id.extraInfo_favor_control).setVisibility(View.VISIBLE); // nav mode, hide 'favor'
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
                if(isNavMode) {
                    Toast.makeText(self, getString(R.string.nav_please_leave_first), Toast.LENGTH_SHORT).show();
                    return true;
                }
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
                if(isNavMode) {
                    Toast.makeText(self, getString(R.string.nav_please_leave_first), Toast.LENGTH_SHORT).show();
                    return true;
                }
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

    // ---- inner class START ----
    // use marker clusterer - start
    public class MyItem implements ClusterItem {
        private final LatLng mPosition;

        public MyItem(double lat, double lng) {
            mPosition = new LatLng(lat, lng);
        }

        @Override
        public LatLng getPosition() {
            return mPosition;
        }
    }
    // usr marker clusterer - end
    // ---- inner class END ----
}