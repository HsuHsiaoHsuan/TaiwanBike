package idv.funnybrain.bike;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Created by freeman on 2014/2/14.
 */
class BikeStationDetailFragment extends DialogFragment implements IXmlDownloader {
    private static final boolean D = false;
    private static final String TAG = "BikeStationDetailFragment";

    private String id = "";
    private LinearLayout progressBar;
    private TextView availableBike;
    private TextView availableParking;

    private String bike;
    private String parking;

    private Handler handler;
    private final int MSG_DOWNLOAD_OK = 999;

    //private static String MODE = "";

    static BikeStationDetailFragment newInstance(String mode, IStation station) {
        BikeStationDetailFragment fragment = new BikeStationDetailFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable("station", (XmlParser_Bike.Station)station);
        fragment.setArguments(bundle);
        //MODE = mode;

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(D) Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case MSG_DOWNLOAD_OK:
                        if(D) Log.d(TAG, "handlMessage, I get message!");
                        progressBar.setVisibility(View.GONE);

                        XmlParser_Bike.Station station = getArguments().getParcelable("station");
                        //availableBike.setText(station.getAVAILABLE_BIKE());
                        availableBike.setVisibility(View.VISIBLE);
                        availableBike.setText(bike);
                        availableParking.setVisibility(View.VISIBLE);
                        availableParking.setText(parking);
                        if(D) Log.d(TAG, bike);
                        if(D) Log.d(TAG, parking);
                        break;
                }
            }
        };
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if(D) Log.d(TAG, "onCreateDialog");
        final XmlParser_Bike.Station station = getArguments().getParcelable("station");
        View v = getActivity().getLayoutInflater().inflate(R.layout.fragment_bikedetail, null);

        id = station.getID();
        if(D) Log.d(TAG, "ID to show: " + id);

        progressBar = (LinearLayout) v.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        availableBike = (TextView) v.findViewById(R.id.availableBike);
        availableBike.setText(station.getAVAILABLE_BIKE());

        availableParking = (TextView) v.findViewById(R.id.availableParking);
        availableParking.setText(station.getAVAILABLE_PARKING());

        TextView address = (TextView) v.findViewById(R.id.address);
        address.setText(station.getADDRESS());

        TextView data = (TextView) v.findViewById(R.id.data);
        data.setText(station.getDESCRIBE());

        if(!station.getMAP().equals("")) {
            ImageView map = (ImageView) v.findViewById(R.id.map);
            loadBitmap(station.getMAP(), map);
        }

        if(!station.getPIC_LARGE().equals("")) {
            ImageView streetView = (ImageView) v.findViewById(R.id.streetView);
            loadBitmap(station.getPIC_LARGE(), streetView);
        }

        View titleView = getActivity().getLayoutInflater().inflate(R.layout.fragment_bikedetail_title, null);
        TextView title = (TextView) titleView.findViewById(R.id.title);
        title.setText(station.getNAME());

//        ImageButton bt_map = (ImageButton) titleView.findViewById(R.id.show_on_map);
//        bt_map.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(getActivity(), BikeStationMapActivity.class);
//                Bundle bundle = new Bundle();
//                bundle.putParcelable("station", station);
//                intent.putExtras(bundle);
//                startActivity(intent);
//            }
//        });
        //if(MODE.equals(Utils.fromMap)) {
            //bt_map.setVisibility(View.GONE);
        //}

        ImageButton bt_refresh = (ImageButton) titleView.findViewById(R.id.refresh);
        bt_refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                availableBike.setVisibility(View.GONE);
                availableParking.setVisibility(View.GONE);
                //if(MODE.equals(Utils.fromMap)) {
                (new BikeStationMapActivity()).requestData(BikeStationDetailFragment.this);
                //} else {
                //    BikeStationListActivity_v2.requestData(BikeStationDetailFragment.this);
                //}

            }
        });

        DBHelper helper = new DBHelper(this.getActivity());
        if(D) Log.d(TAG, "query id: " + station.getID());
        boolean isFavor = helper.query(station.getID());
        if(isFavor) { // in favorite mode
            return new AlertDialog.Builder(getActivity())
                    .setCustomTitle(titleView)
                    .setView(v)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNeutralButton(R.string.remove_favor, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            DBHelper helper = new DBHelper(getActivity());
                            int result = helper.delete(station.getID());
                            if(result > 0) {
                                (new BikeStationMapActivity()).refreshFavorMarker(station.getID(), false);
                            }
                            if (D) Log.d(TAG, "click remove: " + result);
                        }
                    })
                    .create();
        } else {
            return new AlertDialog.Builder(getActivity())
                    .setCustomTitle(titleView)
                    .setView(v)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNeutralButton(R.string.add_favor, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            DBHelper helper = new DBHelper(getActivity());
                            ContentValues values = new ContentValues();
                            values.put(DBHelper.DB_COL_STATION_ID, station.getID());
                            long result = helper.insert(values);
                            if (result > 0) {
                                (new BikeStationMapActivity()).refreshFavorMarker(station.getID(), true);
                            }
                            if (D) Log.d(TAG, "click favor: " + result);
                        }
                    })
                    .create();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(D) Log.d(TAG, "onCreateView");
        // TODO draw the background color
        //getDialog().getWindow().setBackgroundDrawableResource(R.drawable.dialog_bg);
        //getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public void loadBitmap(String url, ImageView imageView) {
        Bitmap tmp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        if(cancelPotentialWork(url, imageView)) {
            final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
            final AsyncDrawable asyncDrawable =
                    new AsyncDrawable(null, tmp, task);
            imageView.setImageDrawable(asyncDrawable);
            task.execute(url);
        }
    }

    public static boolean cancelPotentialWork(String url, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final String bitmapData = bitmapWorkerTask.url;
            if (bitmapData.equals("") || !bitmapData.equals(url)) {
                bitmapWorkerTask.cancel(true);
            } else {
                return false;
            }
        }
        return true;
    }

    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,
                             BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference =
                    new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private String url = "";

        public BitmapWorkerTask(ImageView imageView) {
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            url = params[0];
            return decodeSampledBitmapFromUrl(url, 500, 500); // width, height
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    public static Bitmap decodeSampledBitmapFromUrl(String url, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        try {
            HttpGet httpGet = new HttpGet(url);
            HttpResponse httpResponse = Utils.getHttpClient().execute(httpGet);
            byte[] result = EntityUtils.toByteArray(httpResponse.getEntity());
            BitmapFactory.decodeByteArray(result, 0, result.length, options);
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            options.inJustDecodeBounds = false;

            return BitmapFactory.decodeByteArray(result, 0, result.length, options);
        } catch(IOException ioe) {
            Log.e(TAG, ioe.toString());
            return null;
        }
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    @Override
    public void downloadOK(boolean isOK) {
        //View titleView = getActivity().getLayoutInflater().inflate(R.layout.fragment_bikedetail, null);
        //XmlParser_Bike.Station station;
        if(D) Log.d(TAG, "downloadOK! original ID: " + id);

        if(isOK) {
//            if(MODE.equals(Utils.fromList) || MODE.equals(Utils.fromFavorList)) {
//                for(XmlParser_Bike.Station s : BikeStationListActivity_v2.station_list) {
//                    if(D) Log.d(TAG, "ID iterator: " + s.getID());
//                    if (s.getID().equals(id)) {
//                        //station = s;
//                        if(D) Log.d(TAG, "we get same id");
//
//                        bike = s.getAVAILABLE_BIKE();
//                        parking = s.getAVAILABLE_PARKING();
//
//                        Message message = new Message();
//                        message.what = MSG_DOWNLOAD_OK;
//                        BikeStationDetailFragment.this.handler.sendMessage(message);
//                        break;
//                    }
//                }
//            } else {
                for(IStation s : BikeStationMapActivity.station_list) {
                    if(D) Log.d(TAG, "ID iterator: " + s.getID());
                    if (s.getID().equals(id)) {
                        if(D) Log.d(TAG, "we get same id");

                        bike = s.getAVAILABLE_BIKE();
                        parking = s.getAVAILABLE_PARKING();

                        Message message = new Message();
                        message.what = MSG_DOWNLOAD_OK;
                        BikeStationDetailFragment.this.handler.sendMessage(message);
                        break;
                    }
//                }
            }
        }
    }

    @Override
    public void onPause() {
        if(D) Log.d(TAG, "onPause");
        super.onPause();
//        if(MODE.equals(Utils.fromList)) {
//            BikeStationListActivity_v2.removeListener(this);
//        } else if (MODE.equals(Utils.fromFavorList)) {
//            BikeStationListActivity_v2.removeListener(this);
//            BikeStationListActivity_v2.requestData();
//        } else {
            BikeStationMapActivity.removeListener(this);
//        }
    }
}