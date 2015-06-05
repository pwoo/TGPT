package pw.com.tgpt;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.EOFException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by PW on 2015-06-05.
 */
public class CityAlarmList implements Serializable, Parcelable {
    private static final long serialVersionUID = 0L;

    private ArrayList<Integer> mCityIDs;

    public CityAlarmList() {
        mCityIDs = new ArrayList<Integer>();
    }

    public CityAlarmList(Parcel in) {
        while (in.dataAvail() > 0) {
            Integer i = new Integer(in.readInt());
            mCityIDs.add(i);
        }
    }

    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException {
       for (Integer i : mCityIDs) {
           out.write(i.intValue());
       }
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        try {
            while (true) {
                Integer i = new Integer(in.readInt());
                mCityIDs.add(i);
            }
        }
        catch (EOFException e) {}
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        for (Integer i : mCityIDs) {
            dest.writeInt(i.intValue());
        }
    }

    public static final Parcelable.Creator<CityAlarmList> CREATOR
            = new Parcelable.Creator<CityAlarmList>() {
        public CityAlarmList createFromParcel(Parcel in) {
            return new CityAlarmList(in);
        }

        public CityAlarmList[] newArray(int size) {
            return new CityAlarmList[size];
        }
    };
}
