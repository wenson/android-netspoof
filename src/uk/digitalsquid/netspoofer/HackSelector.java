package uk.digitalsquid.netspoofer;

import java.util.List;

import uk.digitalsquid.netspoofer.NetSpoofService.NetSpoofServiceBinder;
import uk.digitalsquid.netspoofer.config.LogConf;
import uk.digitalsquid.netspoofer.servicestatus.SpoofList;
import uk.digitalsquid.netspoofer.spoofs.Spoof;
import uk.digitalsquid.netspoofer.spoofs.Spoof.OnExtraDialogDoneListener;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class HackSelector extends Activity implements OnItemClickListener, LogConf {
	ProgressDialog startingProgress;
	
	private ListView spoofList;
	
	boolean haveSpoofList = false;
	boolean gettingSpoofList = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.hackselector);
        startService(new Intent(this, NetSpoofService.class));
        
        spoofListAdapter = new SpoofListAdapter();
        spoofList = (ListView) findViewById(R.id.spoofList);
        spoofList.setAdapter(spoofListAdapter);
        spoofList.setOnItemClickListener(this);
        
	    statusFilter = new IntentFilter();
	    statusFilter.addAction(NetSpoofService.INTENT_STATUSUPDATE);
	    statusFilter.addAction(NetSpoofService.INTENT_SPOOFLIST);
		registerReceiver(statusReceiver, statusFilter);
		
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
        stopService(new Intent(this, NetSpoofService.class));
		unregisterReceiver(statusReceiver);
	}

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        bindService(new Intent(this, NetSpoofService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (service != null) {
            unbindService(mConnection);
            service = null;
        }
    }
	
	private NetSpoofService service;
	
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			NetSpoofServiceBinder binder = (NetSpoofServiceBinder) service;
            HackSelector.this.service = binder.getService();
            
            switch(HackSelector.this.service.getStatus()) {
            case NetSpoofService.STATUS_LOADING:
            	showStartingDialog();
            	break;
            case NetSpoofService.STATUS_LOADED:
            	if(!gettingSpoofList) {
            		gettingSpoofList = true;
            		HackSelector.this.service.requestSpoofs();
            	}
            	break;
            }
		}
		
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			service = null;
		}
	};
	
	private IntentFilter statusFilter;
	private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(NetSpoofService.INTENT_STATUSUPDATE)) {
				switch(intent.getIntExtra(NetSpoofService.INTENT_EXTRA_STATUS, NetSpoofService.STATUS_FINISHED)) {
				case NetSpoofService.STATUS_LOADING:
					showStartingDialog();
					break;
				case NetSpoofService.STATUS_LOADED:
					if(startingDialog != null) startingDialog.cancel();
	            	if(!gettingSpoofList) {
	            		gettingSpoofList = true;
	            		HackSelector.this.service.requestSpoofs();
	            	}
					break;
				case NetSpoofService.STATUS_FAILED:
					// FIXME: Handle this.
					break;
				}
			} else if(intent.getAction().equals(NetSpoofService.INTENT_SPOOFLIST)) {
				SpoofList spoofs = (SpoofList) intent.getSerializableExtra(NetSpoofService.INTENT_EXTRA_SPOOFLIST);
				spoofListAdapter.setSpoofs(spoofs.getSpoofs());
			}
		}
	};
	
	private ProgressDialog startingDialog;
	
	private void showStartingDialog() {
		if(startingDialog != null) if(startingDialog.isShowing()) return;
		startingDialog = new ProgressDialog(this);
		startingDialog.setTitle(R.string.loading);
		startingDialog.setMessage("Starting environment... This should take a few seconds");
		startingDialog.setCancelable(false);
		startingDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
		    @Override
		    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
		        if (keyCode == KeyEvent.KEYCODE_SEARCH && event.getRepeatCount() == 0) {
		            return true;
		        }
		        return false;
		    }
		});
		startingDialog.show();
	}
	
	private SpoofListAdapter spoofListAdapter;
	
	private class SpoofListAdapter extends BaseAdapter {
		private final LayoutInflater inflater;
		
		private List<Spoof> spoofs;
		
		public SpoofListAdapter() {
			inflater = LayoutInflater.from(HackSelector.this);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
            if(spoofs == null) {
		        if (convertView == null) {
		            convertView = inflater.inflate(R.layout.listloadingitem, null);
		            convertView.setEnabled(false);
		        }
            } else {
		        if (convertView != null) {
		        	if(convertView.findViewById(R.id.spoofTitle) == null) // Must be other view
			            convertView = inflater.inflate(R.layout.spoofitem, null);
		        } else {
		            convertView = inflater.inflate(R.layout.spoofitem, null);
		        }
		        TextView title = (TextView) convertView.findViewById(R.id.spoofTitle);
		        TextView description = (TextView) convertView.findViewById(R.id.spoofDescription);
		        
	        	title.setText(spoofs.get(position).getTitle());
	        	description.setText(spoofs.get(position).getDescription());
            }

            return convertView;
		}
		
		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public Spoof getItem(int position) {
			if(spoofs == null) return null;
			return spoofs.get(position);
		}
		
		@Override
		public int getCount() {
			if(spoofs == null) return 1;
			return spoofs.size();
		}
		
		public void setSpoofs(List<Spoof> spoofs) {
			this.spoofs = spoofs;
			notifyDataSetChanged();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
		final Spoof spoof = spoofListAdapter.getItem(position);
		if(spoof == null) return;
		
		// Start processing spoof etc.
		final OnExtraDialogDoneListener onDone = new OnExtraDialogDoneListener() {
			@Override
			public void onDone() {
				Log.d(TAG, "Dialog done, continuing");
				Intent intent = new Intent(HackSelector.this, RouterSelector.class);
				intent.putExtra(RouterSelector.EXTRA_SPOOF, spoof);
				startActivity(intent);
			}
		};
		Dialog optDialog = spoof.displayExtraDialog(this, onDone);
		if(optDialog == null) { // Null, so execute onDone now.
			onDone.onDone();
		} else { // Let dialog do so.
			optDialog.show();
		}
	}
}