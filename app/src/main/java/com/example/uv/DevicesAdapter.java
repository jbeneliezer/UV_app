package com.example.uv;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

// Create the basic adapter extending from RecyclerView.Adapter
// Note that we specify the custom ViewHolder which gives us access to our views
public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.ViewHolder> {
    // Store a member variable for the found devices
    private List<BluetoothDevice> mDevices;

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public class ViewHolder extends RecyclerView.ViewHolder {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public TextView nameDevice;
        public Button buttonConnect;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            nameDevice = (TextView) itemView.findViewById(R.id.name_device);
            buttonConnect = (Button) itemView.findViewById(R.id.button_connect);
        }
    }

    // Pass in the contact array into the constructor
    public DevicesAdapter(List<BluetoothDevice> devices) {
        mDevices = devices;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.bluetooth_recycler_row, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(contactView);
        return viewHolder;
    }

    // Involves populating data into the item through holder
    @SuppressLint("MissingPermission")
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // Get the data model based on position
        BluetoothDevice device = mDevices.get(position);

        // Set item views based on your views and data model
        TextView textView = holder.nameDevice;
        textView.setText(device.getName());
        Button button = holder.buttonConnect;
        button.setText("Pair");
    }

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return mDevices.size();
    }
}
