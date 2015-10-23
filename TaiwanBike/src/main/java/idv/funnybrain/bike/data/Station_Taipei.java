package idv.funnybrain.bike.data;

/**
 * Created by Freeman on 2014/5/17.
 */
public class Station_Taipei implements IStation {
    private String sno;
    private String sna;
    private int tot;
    private String sbi;
    private String sarea;
    private double mday;
    private String lat;
    private String lng;
    private String ar;
    private String sareaen;
    private String snaen;
    private String aren;
    private String bemp;
    private int act;

    public String getSno() { return sno; }

    public void setSno(String sno) { this.sno = sno; }

    public String getSna() { return sna; }

    public void setSna(String sna) { this.sna = sna; }

    public int getTot() { return tot; }

    public void setTot(int tot) { this.tot = tot; }

    public String getSbi() { return sbi; }

    public void setSbi(String sbi) { this.sbi = sbi; }

    public String getSarea() { return sarea; }

    public void setSarea(String sarea) { this.sarea = sarea; }

    public double getMday() { return mday; }

    public void setMday(double mday) { this.mday = mday; }

    public String getLat() { return lat; }

    public void setLat(String lat) {this.lat = lat;}

    public String getLng() { return lng; }

    public void setLng(String lng) { this.lng = lng; }

    public String getAr() { return ar; }

    public void setAr(String ar) { this.ar = ar; }

    public String getSareaen() { return sareaen; }

    public void setSareaen(String sareaen) { this.sareaen = sareaen; }

    public String getSnaen() { return snaen; }

    public void setSnaen(String snaen) { this.snaen = snaen; }

    public String getAren() { return aren; }

    public void setAren(String aren) { this.aren = aren; }

    public String getBemp() { return bemp; }

    public void setBemp(String bemp) { this.bemp = bemp; }

    public int getAct() { return act; }

    public void setAct(int act) { this.act = act; }


    @Override
    public String getID() {
        return this.sno;
    }

    @Override
    public String getNO() {
        return this.sno;
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

    @Override
    public String getNAME() {
        return this.sna;
    }

    @Override
    public String getNAME_eng() {
        return this.snaen;
    }

    @Override
    public String getAVAILABLE_BIKE() {
        return this.sbi;
    }

    @Override
    public String getLAT() {
        return this.lat;
    }

    @Override
    public String getLON() {
        return this.lng;
    }

    @Override
    public String getADDRESS() {
        return this.ar;
    }

    @Override
    public String getADDRESS_eng() {
        return this.aren;
    }

    @Override
    public String getDistrict() {
        return this.sarea;
    }

    @Override
    public String getDistrict_eng() {
        return this.sareaen;
    }

    @Override
    public String getAVAILABLE_PARKING() {
        return this.bemp;
    }
}


/*
      "iid": "339",
      "sv": "1",
      "sd": "20000101000000",
      "vtyp": "1",
      "sno": "0001",
      "sna": "捷運市政府站(3號出口)",
      "sip": "10.7.0.11",
      "tot": "180",
      "sbi": "4",
      "sarea": "信義區",
      "mday": "20140517183524",
      "lat": "25.0408578889",
      "lng": "121.567904444",
      "ar": "忠孝東路/松仁路(東南側)",
      "sareaen": "Xinyi Dist.",
      "snaen": "MRT Taipei City Hall Stataion(Exit 3)-2",
      "aren": "The S.W. side of Road Zhongxiao East Road & Road Chung Yan.",
      "nbcnt": "0",
      "bemp": "175",
      "act": "1"
 */