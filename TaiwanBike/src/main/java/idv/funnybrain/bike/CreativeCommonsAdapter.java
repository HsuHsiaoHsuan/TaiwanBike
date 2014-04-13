package idv.funnybrain.bike;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by Freeman on 2014/3/9.
 */
public class CreativeCommonsAdapter extends BaseAdapter {
    int[] icon = {
            R.drawable.cc_bicycle,
            R.drawable.cc_cloud31,
            R.drawable.cc_heart3,
            R.drawable.cc_note16,
            R.drawable.cc_pins9,
            R.drawable.cc_silhouette4,
            //R.drawable.cc_cycling,
            R.drawable.cc_car63,
            R.drawable.cc_train6,
            R.drawable.cc_checkered4,
            R.drawable.cc_door13,
            R.drawable.cc_arrow47
    };
    String[] source = {
            "Icon made by Freepik from Flaticon.com",
            "Icon made by Yannick from Flaticon.com",
            "Icon made by Designmodo from Flaticon.com",
            "Icon made by SimpleIcon from Flaticon.com",
            "Icon made by Freepik from Flaticon.com",
            "Icon made by Scott de Jonge from Flaticon.com",
            //"Icon made by Scott de Jonge from Flaticon.com",
            "Icon made by Scott de Jonge from Flaticon.com",
            "Icon made by Freepik from Flaticon.com",
            "Icon made by Freepik from Flaticon.com",
            "Icon made by Freepik from Flaticon.com",
            "Icon made by Stephen Hutchings from Flaticon.com"
    };

    private final LayoutInflater mInflater;

    CreativeCommonsAdapter(LayoutInflater inflater) {
        mInflater = inflater;
    }

    @Override
    public int getCount() {
        return icon.length;
    }

    @Override
    public Object getItem(int position) {
        return icon[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHoler {
        public ImageView icon;
        public TextView source;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        if(rowView == null) {
            rowView = mInflater.inflate(R.layout.dialog_cc, null);
            ViewHoler viewHoler = new ViewHoler();
            viewHoler.icon = (ImageView) rowView.findViewById(R.id.icon);
            viewHoler.source = (TextView) rowView.findViewById(R.id.source);
            rowView.setTag(viewHoler);
        }

        ViewHoler holder = (ViewHoler) rowView.getTag();
        holder.icon.setImageResource(icon[position]);
        holder.source.setText(source[position]);
        /*
        View rowView = convertView;
        if(rowView == null) {
            rowView = layoutInflater.inflate(R.layout.bikelist_cell, null);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.title = (TextView) rowView.findViewById(R.id.title);
            viewHolder.bikes = (TextView) rowView.findViewById(R.id.bikes);
            viewHolder.parking = (TextView) rowView.findViewById(R.id.parking);
            rowView.setTag(viewHolder);
        }
        //System.out.println(stations.get(position).getNAME() + "!");

        ViewHolder holder = (ViewHolder) rowView.getTag();
        holder.title.setText(stations.get(position).getNAME());
        holder.bikes.setText(stations.get(position).getAVAILABLE_BIKE());
        holder.parking.setText(stations.get(position).getAVAILABLE_PARKING());
         */
        return rowView;
    }
}