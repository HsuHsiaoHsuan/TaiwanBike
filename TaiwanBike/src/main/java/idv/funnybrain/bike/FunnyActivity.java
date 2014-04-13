package idv.funnybrain.bike;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.google.android.gms.common.GooglePlayServicesUtil;

/**
 * Created by Freeman on 2014/3/17.
 */
public class FunnyActivity extends FragmentActivity {

    //private ListView listView;
    FunnyActivity self;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_v2);
        self = this;

        ListView listView = (ListView) findViewById(R.id.main_v2_list);

        String[] list_content = {
                getResources().getString(R.string.tai_or_kao),
                getResources().getString(R.string.back_to_map),
                getResources().getString(R.string.cc),
                getResources().getString(R.string.map_license),
                getResources().getString(R.string.close_app)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list_content);

        listView.setAdapter(adapter);


        final Intent intent = new Intent(FunnyActivity.this, BikeStationMapActivity.class);
        //startActivity(intent);

        int mode = (new Utils()).getMapSourceMode(this);
        if(mode == 9) {
            //Toast.makeText(this, "HIHI", Toast.LENGTH_SHORT).show();
            new AlertDialog.Builder(this)
                           .setTitle(R.string.tai_or_kao)
                           .setMessage(R.string.tai_or_kao_msg)
                           .setPositiveButton(R.string.kaohsiung, new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface dialog, int which) {
                                   (new Utils()).setMapSourceMode(self, 1);
                                   setUpKaohsiung();
                                   intent.putExtra("MODE", 1);
                                   startActivity(intent);
                               }
                           })
                           .setNegativeButton(R.string.taipei, new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface dialog, int which) {
                                   (new Utils()).setMapSourceMode(self, 2);
                                   setUpTaipei();
                                   intent.putExtra("MODE", 2);
                                   startActivity(intent);
                               }
                           })
                           .show();
        } else {
            startActivity(intent);
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        new AlertDialog.Builder(self)
                                .setTitle(R.string.tai_or_kao)
                                .setMessage(R.string.tai_or_kao_msg)
                                .setPositiveButton(R.string.kaohsiung, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        (new Utils()).setMapSourceMode(self, 1);
                                        setUpKaohsiung();
                                        intent.putExtra("MODE", 1);
                                        startActivity(intent);
                                    }
                                })
                                .setNegativeButton(R.string.taipei, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        (new Utils()).setMapSourceMode(self, 2);
                                        setUpTaipei();
                                        intent.putExtra("MODE", 2);
                                        startActivity(intent);
                                    }
                                })
                                .show();
                        break;
                    case 1:
                        Intent intent = new Intent(FunnyActivity.this, BikeStationMapActivity.class);
                        startActivity(intent);
                        break;
                    case 2:
                        ListView list = new ListView(self);
                        list.setAdapter(new CreativeCommonsAdapter(getLayoutInflater()));
                        new AlertDialog.Builder(self)
                                .setTitle(R.string.cc)
                                .setView(list)
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                        break;
                    case 3:
                        new AlertDialog.Builder(self)
                                .setTitle(R.string.map_license)
                                .setMessage(GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(self))
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                        break;
                    case 4:
                        finish();
                        break;
                }
            }
        });
    }

    private void setUpKaohsiung() {
        SharedPreferences lastLoction = getSharedPreferences("Location", 0);
        SharedPreferences.Editor editor = lastLoction.edit();
        editor.putFloat("zoom", 12.0f);
        editor.putString("lat", String.valueOf(22.656898));
        editor.putString("lon", String.valueOf(120.315129));
        editor.commit();
    }

    private void setUpTaipei() {
        SharedPreferences lastLoction = getSharedPreferences("Location", 0);
        SharedPreferences.Editor editor = lastLoction.edit();
        editor.putFloat("zoom", 12.0f);
        editor.putString("lat", String.valueOf(25.048436));
        editor.putString("lon", String.valueOf(121.537063));
        editor.commit();
    }
}
