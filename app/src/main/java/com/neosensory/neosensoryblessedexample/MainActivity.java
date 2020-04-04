package com.neosensory.neosensoryblessedexample;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.neosensory.neosensoryblessed.NeosensoryBLESSED;

public class MainActivity extends AppCompatActivity {
  private final String TAG = MainActivity.class.getSimpleName();
  private TextView neoCLIOutput;
  private TextView neoCLIHeader;
  private Button neoConnectButton;
  private Button neoVibrateButton;
  private static final int REQUEST_ENABLE_BT = 1;
  private static final int ACCESS_LOCATION_REQUEST = 2;
  private static final int ACCESS_COARSE_LOCATION_REQUEST = 2;
  private NeosensoryBLESSED blessedNeo;
  private static boolean vibrating = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    setContentView(R.layout.activity_main);
    neoCLIOutput = (TextView) findViewById(R.id.cli_response);
    neoCLIOutput.setVisibility(View.INVISIBLE);
    neoCLIHeader = (TextView) findViewById(R.id.cli_header);
    neoCLIHeader.setVisibility(View.INVISIBLE);
    neoVibrateButton = (Button) findViewById(R.id.pattern_button);
    neoVibrateButton.setVisibility(View.INVISIBLE);
    neoVibrateButton.setClickable(false);
    neoConnectButton = (Button) findViewById(R.id.connection_button);
    neoConnectButton.setVisibility(View.INVISIBLE);
    neoConnectButton.setClickable(false);

    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    if (bluetoothAdapter == null) return;
    if (!bluetoothAdapter.isEnabled()) {
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }
    if (hasPermissions()) {
      neoConnectButton.setClickable(true);
      neoConnectButton.setVisibility(View.VISIBLE);
      neoConnectButton.setOnClickListener(
          new View.OnClickListener() {
            public void onClick(View v) {
              initBluetoothHandler();
            }
          });
    }
  }

  private void initBluetoothHandler() {
    blessedNeo = NeosensoryBLESSED.getInstance(getApplicationContext(), false);
    registerReceiver(CLIReceiver, new IntentFilter("CLIOutput"));
    registerReceiver(ConnectionStateReceiver, new IntentFilter("ConnectionState"));
    registerReceiver(CLIReadyReceiver, new IntentFilter("CLIAvailable"));
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    unregisterReceiver(CLIReceiver);
    unregisterReceiver(ConnectionStateReceiver);
    unregisterReceiver(CLIReadyReceiver);
  }

  private final BroadcastReceiver CLIReadyReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          Boolean CLIState = (Boolean) intent.getSerializableExtra("CLIReady");
          // Prior to calling other API commands we need to accept the Neosensory API ToS
          blessedNeo.sendAPIAuth();
          blessedNeo.acceptAPIToS();
          Log.i(TAG, String.format("state message", blessedNeo.getNeoCLIResponse()));
          // assuming successful authorization:

          neoVibrateButton.setVisibility(View.VISIBLE);
          neoVibrateButton.setClickable(true);
          neoVibrateButton.setOnClickListener(
              new View.OnClickListener() {
                public void onClick(View v) {
                  if (!vibrating) {
                    neoVibrateButton.setText("Stop Vibration Pattern");
                    blessedNeo.stopAudio();
                    blessedNeo.clearMotorQueue();
                    blessedNeo.startMotors();
                    blessedNeo.vibrateMotors(
                        new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255});
                    vibrating = true;
                  } else {
                    neoVibrateButton.setText("Start Vibration Pattern");
                    blessedNeo.vibrateMotors(new byte[] {(byte) 0, (byte) 0, (byte) 0, (byte) 0});
                    vibrating = false;
                  }
                }
              });
        }
      };

  private final BroadcastReceiver ConnectionStateReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          Boolean connectedState = (Boolean) intent.getSerializableExtra("connectedState");
          if (connectedState == true) {
            neoCLIOutput.setVisibility(View.VISIBLE);
            neoCLIHeader.setVisibility(View.VISIBLE);
            neoConnectButton.setText("Disconnect");
            neoConnectButton.setOnClickListener(
                new View.OnClickListener() {
                  public void onClick(View v) {
                    blessedNeo.disconnectNeoDevice();
                  }
                });
          } else {
            neoCLIOutput.setVisibility(View.INVISIBLE);
            neoCLIHeader.setVisibility(View.INVISIBLE);
            neoVibrateButton.setVisibility(View.INVISIBLE);
            neoVibrateButton.setClickable(false);
            neoConnectButton.setText("Scan and Connect to Neosensory Buzz");
            neoConnectButton.setOnClickListener(
                new View.OnClickListener() {
                  public void onClick(View v) {
                    blessedNeo.attemptNeoReconnect();
                  }
                });
          }
        }
      };

  private final BroadcastReceiver CLIReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          String notification_value = (String) intent.getSerializableExtra("CLIResponse");
          neoCLIOutput.setText(notification_value);
        }
      };

  private boolean hasPermissions() {
    int targetSdkVersion = getApplicationInfo().targetSdkVersion;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        && targetSdkVersion >= Build.VERSION_CODES.Q) {
      if (getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
          != PackageManager.PERMISSION_GRANTED) {
        requestPermissions(
            new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_LOCATION_REQUEST);
        return false;
      }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
          != PackageManager.PERMISSION_GRANTED) {
        requestPermissions(
            new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, ACCESS_LOCATION_REQUEST);
        return false;
      }
    }
    return true;
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    switch (requestCode) {
      case ACCESS_LOCATION_REQUEST:
        if (grantResults.length > 0) {
          if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initBluetoothHandler();
          }
        }
        break;
      default:
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        break;
    }
  }
}
