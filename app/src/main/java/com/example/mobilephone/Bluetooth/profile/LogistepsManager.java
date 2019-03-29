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

package com.example.mobilephone.Bluetooth.profile;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import com.example.mobilephone.Bluetooth.profile.callback.LogistepsSensorDataCallback;
import com.example.mobilephone.Bluetooth.profile.data.Step;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.log.LogContract;
import no.nordicsemi.android.log.LogSession;
import no.nordicsemi.android.log.Logger;

public class LogistepsManager extends BleManager<LogistepsManagerCallbacks> {
	/** Nordic Blinky Service UUID. */
	public final static UUID LOGISTEPS_UUID_SERVICE = UUID.fromString("00000000-1212-efde-1523-785fef13d123");
	/** BUTTON characteristic UUID. */
	public final static UUID TOP_SENSOR_UUID_DATA_CHAR = UUID.fromString("00001111-1212-efde-1523-785fef13d123");
	/** LED characteristic UUID. */
	public final static UUID BOTTOM_SENSOR_UUID_DATA_CHAR = UUID.fromString("00002222-1212-efde-1523-785fef13d123");

	private BluetoothGattCharacteristic mTopSensorDataCharacteristic, mBottomSensorDataCharacteristic;
	private ArrayList<Step> mStepList;
	private LogSession mLogSession;
	private boolean mSupported;

	public LogistepsManager(@NonNull final Context context) {
		super(context);
	}

	@NonNull
	@Override
	protected BleManagerGattCallback getGattCallback() {
		return mGattCallback;
	}

	/**
	 * Sets the log session to be used for low level logging.
	 * @param session the session, or null, if nRF Logger is not installed.
	 */
	public void setLogger(@Nullable final LogSession session) {
		this.mLogSession = session;
	}

	@Override
	public void log(final int priority, @NonNull final String message) {
		// The priority is a Log.X constant, while the Logger accepts it's log levels.
		Logger.log(mLogSession, LogContract.Log.Level.fromPriority(priority), message);
	}

	@Override
	protected boolean shouldClearCacheWhenDisconnected() {
		return !mSupported;
	}

	/**
	 * The top sensor callback will be notified when the microcontroller pushes data for the
     * top sensor
	 */
	private final LogistepsSensorDataCallback mTopSensorCallback = new LogistepsSensorDataCallback() {
        @Override
        public void onSensorDataRecieved(@NonNull BluetoothDevice device, List<Integer> sensorReadings) {
            //TODO: Create a step object and push to the stepList
        }

        @Override
        public void onInvalidDataReceived(@NonNull final BluetoothDevice device,
                                          @NonNull final Data data) {
            log(Log.WARN, "Invalid data received: " + data);
        }
    };

    /**
     * The bottom sensor callback will be notified when the microcontroller pushes data for the
     * bottom sensor
     */
	private final LogistepsSensorDataCallback mBottomSensorCallback = new LogistepsSensorDataCallback() {
        @Override
        public void onSensorDataRecieved(@NonNull BluetoothDevice device, List<Integer> sensorReadings) {
            //TODO: Create a step object and push to the stepList
        }

        @Override
        public void onInvalidDataReceived(@NonNull final BluetoothDevice device,
                                          @NonNull final Data data) {
            log(Log.WARN, "Invalid data received: " + data);
        }
    };

//	private	final BlinkyButtonDataCallback mButtonCallback = new BlinkyButtonDataCallback() {
//		@Override
//		public void onButtonStateChanged(@NonNull final BluetoothDevice device,
//										 final int data) {
//			mCallbacks.onButtonStateChanged(device, data);
//		}
//
//		@Override
//		public void onInvalidDataReceived(@NonNull final BluetoothDevice device,
//										  @NonNull final Data data) {
//			log(Log.WARN, "Invalid data received: " + data);
//		}
//	};

	/**
	 * The LED callback will be notified when the LED state was read or sent to the target device.
	 * <p>
	 * This callback implements both {@link no.nordicsemi.android.ble.callback.DataReceivedCallback}
	 * and {@link no.nordicsemi.android.ble.callback.DataSentCallback} and calls the same
	 * method on success.
	 * <p>
	 * If the data received were invalid, the
	 * {@link BlinkyLedDataCallback#onInvalidDataReceived(BluetoothDevice, Data)} will be
	 * called.
	 */
//	private final BlinkyLedDataCallback mLedCallback = new BlinkyLedDataCallback() {
//		@Override
//		public void onLedStateChanged(@NonNull final BluetoothDevice device,
//									  final boolean on) {
//			mLedOn = on;
//			log(LogContract.Log.Level.APPLICATION, "LED " + (on ? "ON" : "OFF"));
//			mCallbacks.onLedStateChanged(device, on);
//		}
//
//		@Override
//		public void onInvalidDataReceived(@NonNull final BluetoothDevice device,
//										  @NonNull final Data data) {
//			// Data can only invalid if we read them. We assume the app always sends correct data.
//			log(Log.WARN, "Invalid data received: " + data);
//		}
//	};

	/**
	 * BluetoothGatt callbacks object.
	 */
	private final BleManagerGattCallback mGattCallback = new BleManagerGattCallback() {
		@Override
		protected void initialize() {
			setNotificationCallback(mTopSensorDataCharacteristic).with(mTopSensorCallback);
			setNotificationCallback(mBottomSensorDataCharacteristic).with(mBottomSensorCallback);
			readCharacteristic(mBottomSensorDataCharacteristic).with(mBottomSensorCallback).enqueue();
			readCharacteristic(mTopSensorDataCharacteristic).with(mTopSensorCallback).enqueue();
			enableNotifications(mTopSensorDataCharacteristic).enqueue();
			enableNotifications(mBottomSensorDataCharacteristic).enqueue();
		}

		@Override
		public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
			final BluetoothGattService service = gatt.getService(LOGISTEPS_UUID_SERVICE);
			if (service != null) {
				mTopSensorDataCharacteristic = service.getCharacteristic(TOP_SENSOR_UUID_DATA_CHAR);
				mBottomSensorDataCharacteristic = service.getCharacteristic(BOTTOM_SENSOR_UUID_DATA_CHAR);
			}

			mSupported = mTopSensorDataCharacteristic != null && mBottomSensorDataCharacteristic != null;
			return mSupported;
		}

		@Override
		protected void onDeviceDisconnected() {
			mTopSensorDataCharacteristic = null;
			mBottomSensorDataCharacteristic = null;
		}
	};
}