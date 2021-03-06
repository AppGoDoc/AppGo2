package br.com.appgo.appgo.Services;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class FindAddress {
    Context mContext;
    List<Address> mFindAddress;
    Geocoder geocoder;

    public FindAddress(Context context) {
        this.mContext = context;
        geocoder = new Geocoder(context);
    }

    public List<Address> getmFindAddress(String localAddress){
        try {
            mFindAddress = geocoder.getFromLocationName(localAddress, 5);
        }
        catch (Exception e){
            e.printStackTrace();
            Toast.makeText(mContext, String.format("Impossivel localizar este endereço!\n%s", e.getMessage()),
                    Toast.LENGTH_SHORT).show();
        }
        return mFindAddress;
    }
    public List<Address> getFindLocation(LatLng latLng){
        try{
            mFindAddress = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 5);
        }
        catch (Exception e){
            e.printStackTrace();
            Toast.makeText(mContext, String.format("Impossivel localizar este endereço!\n%s", e.getMessage()),
                    Toast.LENGTH_SHORT).show();
        }
        return mFindAddress;
    }



}
