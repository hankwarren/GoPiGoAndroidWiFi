package com.kgdsoftware.gopigo;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static android.R.attr.direction;
import static android.R.attr.port;

public class MainActivity extends AppCompatActivity implements TouchPad.Listener {
    private static final String TAG = "GP";
    private static int IP_PORT = 33333;
    public static String gopigoAddress = null;
    public static Executor executor = Executors.newSingleThreadExecutor();

    private boolean active = false;
    private View activeView = null;
    private TouchPad touchPad;

    private String pattern = "####.##";
    private DecimalFormat decimalFormat = new DecimalFormat(pattern);
    private boolean forward;
    private boolean left;
    private int speed = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        touchPad = (TouchPad) findViewById(R.id.touch_pad);
        touchPad.setListener(this);
        setTitle("Waiting...");
        executor.execute(new GetAddress());

        SeekBar speedBar = (SeekBar) findViewById(R.id.speedBar);
        assert speedBar != null;
        speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar speedkBar, int progress, boolean fromUser) {
                updateSpeedLabel(progress);
                speed = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // It was too noisy to send the command in onProgressChanged.
                sendCommand("speed " + speed);
            }
        });

        updateSpeedLabel(speed);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.gopigo_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.kill_item:
                sendCommand("kill");
                return true;

            case R.id.say_ip_address_item:
                sendCommand("sayip");
                return true;

            case R.id.show_lidar_item:
                sendCommand("showlidar");
                return true;

            case R.id.auto_pilot_item:
                Intent autoPilotIntent = new Intent(this, AutoPilotActivity.class);
                startActivity(autoPilotIntent);
                return true;
        }
        return false;
    }

    private void updateSpeedLabel(int speed) {
        TextView textView = (TextView) findViewById(R.id.speedView);
        assert textView != null;
        textView.setText(Integer.toString(speed));

        final SeekBar speedBar = (SeekBar) findViewById(R.id.speedBar);
        assert speedBar != null;
        speedBar.setProgress(speed);
    }

    public void increaseClick(View view) {
        speed += 10;
        if (speed > 255)
            speed = 255;
        sendCommand("incspeed");
        updateSpeedLabel(speed);
    }

    public void decreaseClick(View view) {
        speed -= 10;
        if (speed < 35)
            speed = 35;
        sendCommand("decspeed");
        updateSpeedLabel(speed);
    }

    public void forwardClick(View view) {
        if (activeView == view && active) {
            sendCommand("stop");
            active = false;
        } else {
            sendCommand("forward");
            active = true;
        }
        activeView = view;
        forward = true;
    }

    public void backwardClick(View view) {
        if (activeView == view && active) {
            sendCommand("stop");
            active = false;
        } else {
            sendCommand("backward");
            active = true;
        }
        activeView = view;
        forward = false;
    }

    public void stopClick(View view) {
        sendCommand("stop");
        active = false;
        activeView = view;
    }

    public void leftClick(View view) {
        if (activeView == view && active) {
            sendCommand("stop");
            active = false;
        } else {
            sendCommand("left");
            active = true;
        }
        activeView = view;
    }

    public void rightClick(View view) {
        if (activeView == view && active) {
            sendCommand("stop");
            active = false;
        } else {
            sendCommand("right");
            active = true;
        }
        activeView = view;
    }

    public void rotateLeftClick(View view) {
        if (activeView == view && active) {
            sendCommand("stop");
            active = false;
        } else {
            sendCommand("rotl");
            active = true;
        }
        activeView = view;
    }

    public void rotateRightClick(View view) {
        if (activeView == view && active) {
            sendCommand("stop");
            active = false;
        } else {
            sendCommand("rotr");
            active = true;
        }
        activeView = view;
    }

    public void lookLeftClick(View view) {
        sendCommand("sleft");
    }

    public void homeClick(View view) {
        sendCommand("home");
    }

    public void lookRightClick(View view) {
        sendCommand("sright");
    }

    @Override
    public void onUp() {
        Log.v(TAG, "onUp");
        if (this.forward) {
            sendCommand("forward");
        } else {
            sendCommand("backward");
        }
        active = true;
        activeView = null;
    }

    @Override
    public void onDown() {
        Log.v(TAG, "onDown");
    }
    @Override
    public void onMove(double angle, boolean forward, boolean left, double dx, double dy) {
        double length = Math.sqrt(dx * dx + dy * dy);
        Log.v(TAG, "onMove " + decimalFormat.format(angle)
                + " --> " + decimalFormat.format(length));
        if(left != this.left) {
            if(left) {
                sendCommand("left");
            } else {
                sendCommand("right");
            }
            this.left = left;
            active = true;
        }
//        if(forward != this.forward) {
//            if(forward) {
//                executor.execute(new WriteCommand("forward", gopigoAddress));
//            } else {
//                executor.execute(new WriteCommand("backward", gopigoAddress));
//            }
//            this.forward = forward;
//            active = true;
//        }
    }

    // Ask the robot what it;s IP address is.
    public class GetAddress implements Runnable {
        @Override
        public void run() {
            try {
                DatagramSocket socket = new DatagramSocket();
                socket.setBroadcast(true);

                InetAddress address = InetAddress.getByName("255.255.255.255");
                byte[] data = "Hello".getBytes();
                DatagramPacket sendPacket = new DatagramPacket(data, data.length, address, IP_PORT);
                socket.send(sendPacket);
                byte[] bytes = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(bytes, 1024);
                socket.receive(receivePacket);

                Log.v(TAG, "GetAddress - Address: " + receivePacket.getAddress());

                gopigoAddress = receivePacket.getAddress().getHostAddress();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ApplicationInfo applicationInfo = getApplicationInfo();
                        int stringId = applicationInfo.labelRes;
                        String appName = stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : getString(stringId);
                        setTitle(appName + " @" + gopigoAddress);
                    }
                });
            } catch (Exception e) {
                Log.v(TAG, "GetAddress: " + e.getMessage());
            }
        }
    }

    public static void sendCommand(String command) {
        executor.execute(new WriteCommand(command, gopigoAddress));
    }
}
