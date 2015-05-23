package com.sanko.fmap;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;


public class MainActivity extends Activity {
	MapView mMapView = null;
	private LocationMode tempMode = LocationMode.Hight_Accuracy;
	private String tempcoor="bd09ll";
	TextView re = null;
	private SDKReceiver mReceiver;
	public LocationClient mLocationClient = null;
	public BDLocationListener myListener = new MyLocationListener();
	BaiduMap mBaiduMap = null;
	LocationClient mLocClient;
	BitmapDescriptor mCurrentMarker;
	boolean isFirstLoc = true;
	public MyLocationListener mMyLocationListener;
	private double mCurrentLantitude;
	private double mCurrentLongitude;
	protected int mXDirection;  
	/**
	 * ����㲥�����࣬���� SDK key ��֤�Լ������쳣�㲥
	 */
	public class SDKReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			String s = intent.getAction();
			TextView text = (TextView) findViewById(R.id.result_appear);
			text.setTextColor(Color.RED);
			if (s.equals(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR)) {
				text.setText("key ��֤����! ���� AndroidManifest.xml �ļ��м�� key ����");
			} else if (s
					.equals(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR)) {
				text.setText("�������");
			}else{
				text.setText("ok");
			}
		}
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
      //��ʹ��SDK�����֮ǰ��ʼ��context��Ϣ������ApplicationContext
        //ע��÷���Ҫ��setContentView����֮ǰʵ��  
        SDKInitializer.initialize(getApplicationContext());  
        setContentView(R.layout.main);
     // ע�� SDK �㲥������
     		IntentFilter iFilter = new IntentFilter();
     		iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR);
     		iFilter.addAction(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR);
     		mReceiver = new SDKReceiver();
     		registerReceiver(mReceiver, iFilter);
      //��ȡ��ͼ�ؼ�����  
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        re = (TextView)findViewById(R.id.result_appear);
        mLocationClient = new LocationClient(getApplicationContext());     //����LocationClient��
        mLocationClient.registerLocationListener( myListener ); 
        LocationClientOption option = new LocationClientOption();
		option.setLocationMode(tempMode);//���ö�λģʽ
		option.setCoorType(tempcoor);//���صĶ�λ����ǰٶȾ�γ�ȣ�Ĭ��ֵgcj02
		int span=1000;
		option.setScanSpan(span);//���÷���λ����ļ��ʱ��Ϊ5000ms
		option.setIsNeedAddress(true);
		mLocationClient.setLocOption(option);
		mLocationClient.start();
		mLocationClient.requestLocation();
		
		
		
				
				
    
	}
    
    @Override  
    protected void onDestroy() {  
        super.onDestroy();  
        //��activityִ��onDestroyʱִ��mMapView.onDestroy()��ʵ�ֵ�ͼ�������ڹ���  
    
        mMapView.onDestroy(); 
    }  
    @Override  
    protected void onResume() {  
        super.onResume();  
        //��activityִ��onResumeʱִ��mMapView. onResume ()��ʵ�ֵ�ͼ�������ڹ���  
        mMapView.onResume();  
     // ������λͼ��
		mBaiduMap.setMyLocationEnabled(true);
        }  
    @Override  
    protected void onPause() {  
        super.onPause();  
        //��activityִ��onPauseʱִ��mMapView. onPause ()��ʵ�ֵ�ͼ�������ڹ���  
        mMapView.onPause();  
        // ������Ҫ��λͼ��ʱ�رն�λͼ��  
		mBaiduMap.setMyLocationEnabled(false);
        }  
    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
        	center2myLoc();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    public class MyLocationListener implements BDLocationListener {
    	
    	TextView mLocationResult;
    	
    	@Override
    	public void onReceiveLocation(BDLocation location) {
    		if (location == null)
    	            return ;
    		mCurrentLantitude = location.getLatitude();
    		mCurrentLongitude = location.getLongitude();
    		StringBuffer sb = new StringBuffer(256);
//    		sb.append("time : ");
//    		sb.append(location.getTime());
//    		sb.append("\nerror code : ");
//    		sb.append(location.getLocType());
//    		sb.append("\nlatitude : ");
//    		sb.append(location.getLatitude());
//    		sb.append("\nlontitude : ");
//    		sb.append(location.getLongitude());
//    		sb.append("\nradius : ");
//    		sb.append(location.getRadius());
    		if (location.getLocType() == BDLocation.TypeGpsLocation){
    			sb.append("\nspeed : ");
    			sb.append(location.getSpeed());
    			sb.append("\nsatellite : ");
    			sb.append(location.getSatelliteNumber());
    		} else if (location.getLocType() == BDLocation.TypeNetWorkLocation){
//    			sb.append("\naddr : ");
    			sb.append("��ǰλ�ã�"+location.getAddrStr());
    		} 

    		MyLocationData locData = new MyLocationData.Builder()
			.accuracy(location.getRadius())
			.direction(mXDirection)
			.latitude(location.getLatitude())
			.longitude(location.getLongitude()).build();
	mBaiduMap.setMyLocationData(locData); 
//    MyLocationConfiguration config = new MyLocationConfiguration(  
//            mCurrentMode, true, null);  
//    mBaiduMap.setMyLocationConfigeration(config);  
		
	if (isFirstLoc) {
		isFirstLoc = false;
		center2myLoc();
	}
	
	logMsg(sb.toString());
    	}
    	
    	public void logMsg(String str) {
    		try {
    			if (re != null)
    				re.setText(str);
    			
    			
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	}
    }
    
    
    private void center2myLoc()
    {
    	LatLng ll = new LatLng(mCurrentLantitude, mCurrentLongitude);
    	try {
    		MapStatus mMapStatus = new MapStatus.Builder()
            .target(ll)
            .zoom(18.4f)
            .build();
            //����MapStatusUpdate�����Ա�������ͼ״̬��Ҫ�����ı仯
    	    MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
    	    //�ı��ͼ״̬
    	    mBaiduMap.animateMapStatus(mMapStatusUpdate);
		} catch (NumberFormatException e) {
			Toast.makeText(this, "����", Toast.LENGTH_SHORT).show();
		}
    }
    
    
}
