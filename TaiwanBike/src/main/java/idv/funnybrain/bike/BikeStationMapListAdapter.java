package idv.funnybrain.bike;

import android.location.Location;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;
import idv.funnybrain.bike.data.IStation;
import idv.funnybrain.bike.database.DBHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Freeman on 2014/3/7.
 */
public class BikeStationMapListAdapter extends BaseAdapter implements Filterable {
    private static final Boolean D = false;
    private static final String TAG = "BikeStationMapListAdapter";

    private final LayoutInflater layoutInflater;

    private List<IStation> stations;
    private final List<IStation> stations_original;

    private DBHelper helper;

    Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if(D) Log.d(TAG, "Filter, performFiltering");
            FilterResults filterResults = new FilterResults();
            ArrayList<IStation> tmpList = new ArrayList<IStation>();

            if(D) {
                if (constraint == null) Log.d(TAG, "constraint null");
                if (constraint.length() == 0) Log.d(TAG, "constraint 0");
                Log.d(TAG, "constraint: " + constraint);
            }

            if((constraint != null) && (stations != null)) {
                if(constraint.length() == 0) {
                    filterResults.values = stations_original;
                    filterResults.count = stations_original.size();
                } else {
                    for(IStation station : stations_original) {
                        if(station.getNAME().contains(constraint) ||
                           station.getADDRESS().contains(constraint)) {
                            if(D) Log.d(TAG, "Filter, yes! I get one match.");
                            tmpList.add(station);
                        }
                    }
                    filterResults.values = tmpList;
                    filterResults.count = tmpList.size();
                }
            }

            return filterResults;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if(D) Log.d(TAG, "Filter, publicResults");
            stations = (ArrayList<IStation>)results.values;
            if(results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    };

    //public BikeStationMapListAdapter(SherlockListFragment fragment, List<XmlParser_Bike.Station> stationList) {
    public BikeStationMapListAdapter(LayoutInflater inflater, List<IStation> stationList) {
        //layoutInflater = fragment.getSherlockActivity().getLayoutInflater();
        layoutInflater = inflater;
        stations = stationList;
        stations_original = stationList;
        helper = new DBHelper(inflater.getContext());
    }

    @Override
    public int getCount() {
        return stations.size();
    }

    @Override
    public Object getItem(int position) {
        Log.d(TAG, "getItem at position: " + position);
        return stations.get(position);
    }

    @Override
    public long getItemId(int position) {
        //return position;
        return Long.valueOf(stations.get(position).getID());
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    static class ViewHolder {
        //public ImageView is_favor;
        public TextView site_id;
        public TextView title;
        public TextView distance;
        public TextView address;
        public TextView bikes;
        public TextView parking;
        //public RelativeLayout subtitles;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(D) {
            Log.d(TAG, "getView, position: " + position);
        }
        View rowView = convertView;
        if(rowView == null) {
            rowView = layoutInflater.inflate(R.layout.maplist_cell, null);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.site_id = (TextView) rowView.findViewById(R.id.site_id);
            viewHolder.title = (TextView) rowView.findViewById(R.id.title);
            viewHolder.distance = (TextView) rowView.findViewById(R.id.distance_value);
            viewHolder.address = (TextView) rowView.findViewById(R.id.address);
            viewHolder.bikes = (TextView) rowView.findViewById(R.id.bikes);
            viewHolder.parking = (TextView) rowView.findViewById(R.id.parking);
            //viewHolder.subtitles = (RelativeLayout) rowView.findViewById(R.id.sub_title);
            rowView.setTag(viewHolder);
        }
        //System.out.println(stations.get(position).getNAME() + "!");

        final ViewHolder holder = (ViewHolder) rowView.getTag();
        IStation s = stations.get(position);
        holder.site_id.setText(s.getID());
        holder.title.setText(s.getNAME());

        Location location = (new Utils()).getLocation();
        if(location != null) {
            double result = getDistance(location.getLatitude(), location.getLongitude(), Double.valueOf(s.getLAT()), Double.valueOf(s.getLON()));
            holder.distance.setText(formatNumber(result));
            holder.distance.setVisibility(View.VISIBLE);
        }

        holder.address.setText(s.getADDRESS());
        holder.bikes.setText(s.getAVAILABLE_BIKE());
        holder.parking.setText(s.getAVAILABLE_PARKING());

        return rowView;
    }

    public double getDistance(double sLat, double sLon, double dLat, double dLon) {
        LatLng start = new LatLng(sLat, sLon);
        LatLng end = new LatLng(dLat, dLon);

        return SphericalUtil.computeDistanceBetween(start, end);
//        double R = 6378137;
//        double lat = radius(dLat - sLat);
//        double lon = radius(dLon - sLon);
//
//        double x = Math.sin(lat/2) * Math.sin(lat/2) +
//                   Math.cos(radius(sLat)) * Math.cos(radius(dLat)) *
//                   Math.sin(lon/2) * Math.sin(lon/2);
//        double y = 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1-x));
//        return R * y;
    }

    private String formatNumber(double distance) {
        //String unit = "m";
        String unit = layoutInflater.getContext().getResources().getString(R.string.meter);
        if (distance < 1) {
            distance *= 1000;
            //unit = "mm";
            unit = layoutInflater.getContext().getResources().getString(R.string.millimeter);
        } else if (distance > 1000) {
            distance /= 1000;
            //unit = "km";
            unit = layoutInflater.getContext().getResources().getString(R.string.kilometer);
        }

        return String.format("%4.3f%s", distance, unit);
    }

//    public double radius(double d) {
//        return d * Math.PI / 180.0;
//    }
}
