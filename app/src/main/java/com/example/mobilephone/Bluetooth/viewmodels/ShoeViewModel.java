/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.example.mobilephone.Bluetooth.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;

import com.example.mobilephone.Bluetooth.adapter.DiscoveredBluetoothDevice;
import com.example.mobilephone.Bluetooth.profile.LogistepsManager;
import com.example.mobilephone.Bluetooth.profile.LogistepsManagerCallbacks;
import com.example.mobilephone.Managers.OnStepCreatedEventListener;
import com.example.mobilephone.Managers.StepManager;
import com.example.mobilephone.Models.Step;
import com.example.mobilephone.Models.SensorReading;
import com.example.mobilephone.Models.Shoe;
import com.example.mobilephone.Models.User;
import com.example.mobilephone.R;
import com.example.mobilephone.Repositories.StepRepository;
import com.example.mobilephone.Repositories.UserRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import androidx.annotation.NonNull;

import no.nordicsemi.android.log.LogSession;
import no.nordicsemi.android.log.Logger;

public class ShoeViewModel extends AndroidViewModel implements LogistepsManagerCallbacks {
	private final LogistepsManager mLogistepsManager;
	private BluetoothDevice mDevice;
	@Inject StepRepository stepRepository;
	@Inject	UserRepository userRepository;
	private StepManager stepManager;
	private Location lastLocation;
	private ArrayList<Step> steps;
	private Shoe shoe;

	private SharedPreferences sharedPreferences;

	private final long MIN_UPDATE_TIME = 1000;
	private final float MIN_UPDATE_DISTANCE = 5;

	private boolean isConnecting = false;

	// Connection states Connecting, Connected, Disconnecting, Disconnected etc.
	private final MutableLiveData<String> mConnectionState = new MutableLiveData<>();

	// Flag to determine if the device is connected
	private final MutableLiveData<Boolean> mIsConnected = new MutableLiveData<>();

	// Flag to determine if the device has required services
	private final MutableLiveData<Boolean> mIsSupported = new MutableLiveData<>();

	// Flag to determine if the device is ready
	private final MutableLiveData<Void> mOnDeviceReady = new MutableLiveData<>();

	// Flag that holds the pressed released state of the button on the devkit.
	// Pressed is true, Released is false
	private final MutableLiveData<Integer> mButtonState = new MutableLiveData<>();

//	private LocationManager mLocationManager;
//
//	private final LocationListener mLocationListener = new LocationListener() {
//		@Override
//		public void onLocationChanged(Location location) {
//			ShoeViewModel.this.lastLocation = location;
//		}
//
//		@Override
//		public void onStatusChanged(String provider, int status, Bundle extras) {
//
//		}
//
//		@Override
//		public void onProviderEnabled(String provider) {
//
//		}
//
//		@Override
//		public void onProviderDisabled(String provider) {
//
//		}
//	};

	public LiveData<Boolean> isConnected() {
		return mIsConnected;
	}

	public LiveData<Integer> getButtonState() {
		return mButtonState;
	}

	@Inject
	public ShoeViewModel(@NonNull final Application application) {
		super(application);

		sharedPreferences = application.getSharedPreferences("userCredentials", Context.MODE_PRIVATE);

		steps = new ArrayList<>();

		//Create step manager. Collects sensor readings and creates steps
        stepManager = new StepManager();
        stepManager.setOnStepCreatedEventListener(step -> {
        	step.setShoe(shoe.getFoot());
            steps.add(step);
            if(steps.size() >= 10) {
                postSteps();
                steps.clear();
            }
        });

        // Initialize the manager
        mLogistepsManager = new LogistepsManager(getApplication(), stepManager);
        mLogistepsManager.setGattCallbacks(this);

//		mLocationManager = (LocationManager) application.getSystemService(Context.LOCATION_SERVICE);
//		if (application.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
//				&& application.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//			// TODO: Consider calling
//			//    Activity#requestPermissions
//			// here to request the missing permissions, and then overriding
//			//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//			//                                          int[] grantResults)
//			// to handle the case where the user grants the permission. See the documentation
//			// for Activity#requestPermissions for more details.
//			//return TODO;
//		}
//		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_UPDATE_TIME,
//				MIN_UPDATE_DISTANCE, mLocationListener);
	}

	/**
	 * Connect to peripheral.
	 */
	public void connect(@NonNull final DiscoveredBluetoothDevice device) {
		// Prevent from calling again when called again (screen orientation changed)
		if (mDevice == null) {
			mDevice = device.getDevice();
			final LogSession logSession
					= Logger.newSession(getApplication(), null, device.getAddress(), device.getName());
			mLogistepsManager.setLogger(logSession);
			reconnect();
		}
	}

	/**
	 * Reconnects to previously connected device.
	 * If this device was not supported, its services were cleared on disconnection, so
	 * reconnection may help.
	 */
	public void reconnect() {
		if (mDevice != null) {
			mLogistepsManager.connect(mDevice)
					.retry(3, 100)
					.useAutoConnect(false)
					.enqueue();
		}
	}

	/**
	 * Disconnect from peripheral.
	 */
	public void disconnect() {
		mDevice = null;
		mLogistepsManager.disconnect().enqueue();
	}

    public void postSteps() {
        try {
            User currentUser = userRepository.getUser(
                    sharedPreferences.getString("username", ""),
                    sharedPreferences.getString("password", "")
            );

            ArrayList<Step> temp = new ArrayList<>();
            temp.addAll(steps);

            stepRepository.postSteps(temp, currentUser);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

//	public void postSteps(String datetime, List<SensorReading> readings, com.example.mobilephone.Models.Location location) {
//        List<Step> steps = new ArrayList<>();
//
//		try {
//			User currentUser = userRepository.getUser(
//					sharedPreferences.getString("username", ""),
//					sharedPreferences.getString("password", "")
//			);
//
//			steps.add(new Step(datetime, shoe.getFoot(), readings, location));
//			stepRepository.postSteps(steps, currentUser);
//		} catch (ExecutionException e) {
//			e.printStackTrace();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//    }


	@Override
	protected void onCleared() {
		super.onCleared();
		if (mLogistepsManager.isConnected()) {
			disconnect();
		}
	}

    @Override
    public void onSensorDataRecieved(@NonNull BluetoothDevice device, List<Integer> sensorReadings) {
        //TODO: Do something when data is received. Maybe not?
    }

	@Override
	public void onDeviceConnecting(@NonNull final BluetoothDevice device) {
		mConnectionState.postValue(getApplication().getString(R.string.state_connecting));
	}

	@Override
	public void onDeviceConnected(@NonNull final BluetoothDevice device) {
		mIsConnected.postValue(true);
		mConnectionState.postValue(getApplication().getString(R.string.state_discovering_services));
	}

	@Override
	public void onDeviceDisconnecting(@NonNull final BluetoothDevice device) {
		mIsConnected.postValue(false);
	}

	@Override
	public void onDeviceDisconnected(@NonNull final BluetoothDevice device) {
		mIsConnected.postValue(false);
	}

	@Override
	public void onLinkLossOccurred(@NonNull final BluetoothDevice device) {
		mIsConnected.postValue(false);
	}

	@Override
	public void onServicesDiscovered(@NonNull final BluetoothDevice device,
									 final boolean optionalServicesFound) {
		mConnectionState.postValue(getApplication().getString(R.string.state_initializing));
	}

	@Override
	public void onDeviceReady(@NonNull final BluetoothDevice device) {
		mIsSupported.postValue(true);
		mConnectionState.postValue(null);
		mOnDeviceReady.postValue(null);
	}

	@Override
	public void onBondingRequired(@NonNull final BluetoothDevice device) {
		// Blinky does not require bonding
	}

	@Override
	public void onBonded(@NonNull final BluetoothDevice device) {
		// Blinky does not require bonding
	}

	@Override
	public void onBondingFailed(@NonNull final BluetoothDevice device) {
		// Blinky does not require bonding
	}

	@Override
	public void onError(@NonNull final BluetoothDevice device,
						@NonNull final String message, final int errorCode) {
		// TODO implement
        if (errorCode == 0) ;
	}

	@Override
	public void onDeviceNotSupported(@NonNull final BluetoothDevice device) {
		mConnectionState.postValue(null);
		mIsSupported.postValue(false);
	}

	public void setShoe(Shoe shoe) {
	    if (shoe != null) {
            this.shoe = shoe;
        }
    }

    public Shoe getShoe() {
	    return this.shoe;
    }

    public void setConnectingStatus(boolean b) {
	    this.isConnecting = b;
    }

    public boolean isConnecting() {
	    return this.isConnecting;
    }

}
