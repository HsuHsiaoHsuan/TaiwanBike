package idv.funnybrain.bike.data;

import java.util.HashMap;

/**
 * Created by Freeman on 2014/4/23.
 */
public interface IParser {
    public void downloadData();
    //public List<IStation> getStations();
    public HashMap<String, IStation> getStationHashMap();
}
