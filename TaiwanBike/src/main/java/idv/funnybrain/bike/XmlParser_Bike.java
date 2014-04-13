package idv.funnybrain.bike;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.Pair;
import nu.xom.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Freeman on 2014/2/12.
 */
public class XmlParser_Bike {
    private static final boolean D = false;
    private static final String TAG = "XmlParser_Bike";
    private final List<IStation> station_list;
    private final HashMap<String, IStation> station_hashmap;

    public XmlParser_Bike() {
        station_list = new ArrayList<IStation>();
        station_hashmap = new HashMap<String, IStation>();

        try {
            Builder parser = new Builder();
            Document document = null;
            // TODO should I cache the file? maybe good!
            Utils utils = new Utils();

            HttpPost httpRequest = new HttpPost(Utils.OPENDATA_BIKE);
            HttpResponse httpResponse = Utils.getHttpClient().execute(httpRequest);

            if(httpResponse.getStatusLine().getStatusCode() == 200) {
                document = parser.build(Utils.OPENDATA_BIKE);
            }

            Element root = document.getRootElement(); // tag: BIKEStationData
            Element root_sec = root.getFirstChildElement("BIKEStation"); // tag: BIKEStation
            Elements stations = root_sec.getChildElements("Station"); // tag: Station

            for(int x=0; x<stations.size(); x++) {
                Element station = stations.get(x);
                station_list.add(getStationItems(station));
                IStation tmpStation = getStationItems(station);
                station_hashmap.put(tmpStation.getID(), tmpStation);
            }

        } catch(ParsingException pe) {
            if(D) Log.d(TAG, pe.toString());
            if(D) pe.printStackTrace();
        } catch(IOException ioe) {
            if(D) Log.d(TAG, ioe.toString());
            if(D) ioe.printStackTrace();
        }
    }

    private IStation getStationItems(Element source) {
        String m_id = "";
        String m_no = "";
        String m_pic_s = "";
        String m_pic_m = "";
        String m_pic_l = "";
        String m_map = "";
        String m_name = "";
        String m_address = "";
        String m_lat = "";
        String m_lon = "";
        String m_describe = "";
        String m_available_bike = "";
        String m_available_parking = "";
        //Station station = new Station();

        Element id = source.getFirstChildElement(Utils.StationID);
        m_id = id.getValue();

        Element no = source.getFirstChildElement(Utils.StationNO);
        m_no = no.getValue();

        Element pic_s = source.getFirstChildElement(Utils.StationPic);
        m_pic_s = pic_s.getValue();

        Element pic_m = source.getFirstChildElement(Utils.StationPic2);
        m_pic_m = pic_m.getValue();

        Element pic_l = source.getFirstChildElement(Utils.StationPic3);
        m_pic_l = pic_l.getValue();

        Element map = source.getFirstChildElement(Utils.StationMap);
        m_map = map.getValue();

        Element name = source.getFirstChildElement(Utils.StationName);
        m_name = name.getValue();

        Element address = source.getFirstChildElement(Utils.StationAddress);
        m_address = address.getValue();

        Element lat = source.getFirstChildElement(Utils.StationLat);
        m_lat = lat.getValue();

        Element lon = source.getFirstChildElement(Utils.StationLon);
        m_lon = lon.getValue();

        Element describe = source.getFirstChildElement(Utils.StationDesc);
        m_describe = describe.getValue();

        Element available_bike = source.getFirstChildElement(Utils.StationNums1);
        m_available_bike = available_bike.getValue();

        Element available_park = source.getFirstChildElement(Utils.StationNums2);
        m_available_parking = available_park.getValue();

        if(D) {
            Log.d(TAG, "DATA: " + m_id + ", \n"
                                + m_no + ", \n"
                                + m_pic_s + ", \n"
                                + m_pic_m + ", \n"
                                + m_pic_l + ", \n"
                                + m_map + ", \n"
                                + m_name + ", \n"
                                + m_address + ", \n"
                                + m_lat + ", \n"
                                + m_lon + ", \n"
                                + m_describe + ", \n"
                                + m_available_bike + ", \n"
                                + m_available_parking);
            Log.d(TAG, "***************\n");
        }

        return new Station(m_id, m_no, m_pic_s, m_pic_m, m_pic_l, m_map, m_name, m_address, m_lat, m_lon, m_describe,
                m_available_bike, m_available_parking);
    }

    public static class Station implements Parcelable, IStation {
        private String ID;               // StationID
        private String NO;               // StationNO
        private String PIC_SMALL;        // StationPic
        private String PIC_MEDIUM;       // StationPic2
        private String PIC_LARGE;        // StationPic3
        private String MAP;              // StationMap
        private String NAME;             // StationName
        private String ADDRESS;          // StationAddress
        private String LAT;              // StationLat
        private String LON;              // StationLon
        private String DESCRIBE;         // StationDesc
        private String AVAILABLE_BIKE;   // StationNums1
        private String AVAILABLE_PARKING;// StationNums2

        public Station(String id, String no, String pic_small, String pic_medium, String pic_large, String map,
                       String name, String address, String lat, String lon, String describe, String available_bike,
                       String available_parking) {
            ID = id;
            NO = no;
            PIC_SMALL = pic_small;
            PIC_MEDIUM = pic_medium;
            PIC_LARGE = pic_large;
            MAP = map;
            NAME = name;
            ADDRESS = address;
            LAT = lat;
            LON = lon;
            DESCRIBE = describe;
            AVAILABLE_BIKE = available_bike;
            AVAILABLE_PARKING = available_parking;
        }

        public Station(Parcel parcel) {
            ID = parcel.readString();
            NO = parcel.readString();
            PIC_SMALL = parcel.readString();
            PIC_MEDIUM = parcel.readString();
            PIC_LARGE = parcel.readString();
            MAP = parcel.readString();
            NAME = parcel.readString();
            ADDRESS = parcel.readString();
            LAT = parcel.readString();
            LON = parcel.readString();
            DESCRIBE = parcel.readString();
            AVAILABLE_BIKE = parcel.readString();
            AVAILABLE_PARKING = parcel.readString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(ID);
            dest.writeString(NO);
            dest.writeString(PIC_SMALL);
            dest.writeString(PIC_MEDIUM);
            dest.writeString(PIC_LARGE);
            dest.writeString(MAP);
            dest.writeString(NAME);
            dest.writeString(ADDRESS);
            dest.writeString(LAT);
            dest.writeString(LON);
            dest.writeString(DESCRIBE);
            dest.writeString(AVAILABLE_BIKE);
            dest.writeString(AVAILABLE_PARKING);
        }

        public static final Creator<Station> CREATOR = new Creator<Station>() {
            @Override
            public Station createFromParcel(Parcel source) {
                return new Station(source);
            }
            @Override
            public Station[] newArray(int size) {
                return new Station[size];
            }
        };

        //public void setID(String id) {this.ID = id;}
        public String getID() {return ID;}

        //public void setNO(String no) {this.NO = no;}
        public String getNO() {return NO;}

        //public void setPIC_SMALL(String pic_small) {this.PIC_SMALL = pic_small;}
        public String getPIC_SMALL() {return PIC_SMALL;}

        //public void setPIC_MEDIUM(String pic_medium) {this.PIC_MEDIUM = pic_medium;}
        public String getPIC_MEDIUM() {return PIC_MEDIUM;}

        //public void setPIC_LARGE(String pic_large) {this.PIC_LARGE = pic_large;}
        public String getPIC_LARGE() {return PIC_LARGE;}

        //public void setMAP(String map) {this.MAP = map;}
        public String getMAP() {return MAP;}

        //public void setNAME(String name) {this.NAME = name;}
        public String getNAME() {return NAME;}

        @Override
        public String getNAME_eng() { return NAME; }

        //public void setADDRESS(String address) {this.ADDRESS = address;}
        public String getADDRESS() {return ADDRESS;}

        @Override
        public String getADDRESS_eng() { return ADDRESS; }

        @Override
        public String getDistrict() { return ADDRESS; }

        @Override
        public String getDistrict_eng() { return ADDRESS;
        }

        //public void setLAT(String lat) {this.LAT = lat;}
        public String getLAT() {return LAT;}

        //public void setLON(String lon) {this.LON = lon;}
        public String getLON() {return LON;}

        //public void setDESCRIBE(String describe) {this.DESCRIBE = describe;}
        public String getDESCRIBE() {return DESCRIBE;}

        //public void setAVAILABLE_BIKE(String available_bike) {this.AVAILABLE_BIKE = available_bike;}
        public String getAVAILABLE_BIKE() {return AVAILABLE_BIKE;}

        //public void setAVAILABLE_PARKING(String available_parking) {this.AVAILABLE_PARKING = available_parking;}
        public String getAVAILABLE_PARKING() {return AVAILABLE_PARKING;}
    }
    /*
    public static class Station {
        public final String ID;               // StationID
        public final String NO;               // StationNO
        public final String PIC_SMALL;        // StationPic
        public final String PIC_MEDIUM;       // StationPic2
        public final String PIC_LARGE;        // StationPic3
        public final String MAP;              // StationMap
        public final String NAME;             // StationName
        public final String ADDRESS;          // StationAddress
        public final String LAT;              // StationLat
        public final String LON;              // StationLon
        public final String DESCRIBE;         // StationDesc
        public final String AVAILABLE_BIKE;   // StationNums1
        public final String AVAILABLE_PARKING;// StationNums2

        public Station(String id, String no, String pic_small, String pic_medium, String pic_large, String map,
                       String name, String address, String lat, String lon, String describe, String available_bike,
                       String available_parking) {
            ID = id;
            NO = no;
            PIC_SMALL = pic_small;
            PIC_MEDIUM = pic_medium;
            PIC_LARGE = pic_large;
            MAP = map;
            NAME = name;
            ADDRESS = address;
            LAT = lat;
            LON = lon;
            DESCRIBE = describe;
            AVAILABLE_BIKE = available_bike;
            AVAILABLE_PARKING = available_parking;
        }
    }*/

    public List<IStation> getStations() {
        return station_list;
    }

    public HashMap<String, IStation> getStationHashMap() {
        return station_hashmap;
    }

    public List<Pair<String, List<IStation>>> getAmazingStations() {
        List<Pair<String, List<IStation>>> all = new ArrayList<Pair<String, List<IStation>>>();
        String[] districts = {"楠梓區","左營區","鼓山區","三民區","苓雅區","新興區","前金區","鹽埕區","前鎮區","旗津區","小港區",
                              "鳳山區","茂林區","甲仙區","六龜區","杉林區","美濃區","內門區","仁武區","田寮區","旗山區","梓官區",
                              "阿蓮區","湖內區","崗山區","茄萣區","路竹區","鳥松區","永安區","燕巢區","大樹區","大寮區","林園區",
                              "彌陀區","橋頭區","大社區","桃源區","那瑪夏區"};


        for(int x=0; x<districts.length; x++) {
            Iterator<IStation> iterator = station_list.iterator();
            List<IStation> tmp = new ArrayList<IStation>();
            if(D) Log.d(TAG, "Districes: " + districts[x]);
            while(iterator.hasNext()) {
                IStation tmpStation = iterator.next();
                if(tmpStation.getADDRESS().contains(districts[x])) {
                    tmp.add(tmpStation);
                    if(D) Log.d(TAG, "add to list: " + tmpStation.getADDRESS());
                }
            }
            all.add(new Pair<String, List<IStation>>(districts[x], tmp));
        }
        return all;
    }

    public IStation getStationById(String id) {
        Iterator<IStation> iterator = station_list.iterator();
        while(iterator.hasNext()) {
            IStation tmpStation = iterator.next();
            if(tmpStation.getID().equals(id)) return tmpStation;
        }
        return null;
    }
}
