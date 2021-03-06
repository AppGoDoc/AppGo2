package br.com.appgo.appgo.View;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Address;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import br.com.appgo.appgo.Controller.MapLocation;
import br.com.appgo.appgo.Fragment.AdressNotFind;
import br.com.appgo.appgo.Fragment.ConfirmLocationFragment;
import br.com.appgo.appgo.Model.Local;
import br.com.appgo.appgo.Model.User;
import br.com.appgo.appgo.R;
import br.com.appgo.appgo.Services.FindAddress;

import static com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom;


public class SearchOnMapActivity extends FragmentActivity
        implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnMarkerDragListener{

    private String TAG_FRAGMENT = "ConfirmLocationFragment";
    private GoogleMap googleMap;
    private MapLocation mapLocation;
    private GoogleApiClient mGoogleApiClient;
    private ProgressBar progressBar;
    private EditText editText;
    private ImageButton button;
    private FindAddress findAddress;
    private List<Address> addressList = null;
    private Marker marker;
    private FloatingActionButton buttonConfirm;
    FirebaseAuth auth = FirebaseAuth.getInstance();
    DatabaseReference userReference = FirebaseDatabase.getInstance().getReference("Anunciantes/"+ auth.getUid());
    User user = new User();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_on_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        editText = (EditText) findViewById(R.id.edtTextAdress);
        button = (ImageButton) findViewById(R.id.button2);
        buttonConfirm = (FloatingActionButton)findViewById(R.id.float_button_confirm);
        buttonConfirm.setEnabled(false);
        buttonConfirm.setVisibility(View.GONE);
        progressBar = (ProgressBar) findViewById(R.id.progressBar2);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mapLocation = new MapLocation(getApplicationContext(), googleMap, mGoogleApiClient);
        findAddress = new FindAddress(this);

        userReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                user = dataSnapshot.getValue(User.class);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (marker != null){
                    marker.remove();
                }
                String stringAddress = editText.getText().toString();
                if (!stringAddress.isEmpty()) {
                    addressList = findAddress.getmFindAddress(stringAddress);
                    if (addressList.isEmpty()){
                        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                        Fragment fragment = getFragmentManager().findFragmentByTag(TAG_FRAGMENT);
                        if (fragment != null)
                            fragmentTransaction.remove(fragment);
                        fragmentTransaction.addToBackStack(TAG_FRAGMENT);
                        DialogFragment dialogFragment = new AdressNotFind();
                        dialogFragment.show(fragmentTransaction, TAG_FRAGMENT);
                    } else {
                        AddressShowList(addressList);
                        ((InputMethodManager) getSystemService(getApplicationContext().INPUT_METHOD_SERVICE))
                                .hideSoftInputFromWindow(editText.getWindowToken(), 0);
                    }
                }
            }
        });

        buttonConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LatLng latLng = marker.getPosition();
                Local local = new Local();
                local.latitude = latLng.latitude;
                local.longitude = latLng.longitude;
                userReference.child("/local").setValue(local);
            }
        });
    }

    private void showProgress(Boolean token) {
        progressBar.setVisibility(token ? View.VISIBLE : View.GONE);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        googleMap.setOnMarkerDragListener(this);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mapLocation.atualizarMapa(googleMap);
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, 1);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Erro de Conexão: "
                    + connectionResult.getErrorCode(), Toast.LENGTH_SHORT).show();
        }
        Intent intent = new Intent(getApplicationContext(), SplashScreen.class);
        startActivity(intent);
    }

    public void AddressShowList(final List<Address> addresses) {
        final String[] addreesName = StreetNames(addresses);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(addreesName, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int item) {
                // Do something with the selection

                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                Fragment fragment = getFragmentManager().findFragmentByTag(TAG_FRAGMENT);
                if (fragment != null)
                    fragmentTransaction.remove(fragment);
                fragmentTransaction.addToBackStack(TAG_FRAGMENT);
                DialogFragment dialogFragment = new ConfirmLocationFragment();
                dialogFragment.show(fragmentTransaction, TAG_FRAGMENT);

                LatLng lng = new LatLng(addresses.get(item).getLatitude(),
                        addresses.get(item).getLongitude());
                googleMap.animateCamera(newLatLngZoom(lng,17.0f));
                marker = googleMap.addMarker(new MarkerOptions()
                                  .icon(BitmapDescriptorFactory.defaultMarker())
                                  .position(lng)
                                  .draggable(true));
                }
            });
        AlertDialog alert = builder.create();
        alert.show();
        buttonConfirm.setEnabled(true);
        buttonConfirm.setVisibility(View.VISIBLE);
    }

    private String[] StreetNames(List<Address> addressList) {
        String[] streetNames = new String[addressList.size()];
        Log.e("ADREESS LIST SIZE", " :::" + addressList.size());
        for(int i = 0; i < addressList.size(); i++){
            Address address = addressList.get(i);
            streetNames[i] = String.format("%s, %s. %s", address.getThoroughfare(), address.getFeatureName(), address.getLocality());
        }
        return streetNames;
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        Toast.makeText(this, "Arraste o marcador até o local desejado.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {

    }
}
