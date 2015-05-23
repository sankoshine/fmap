package com.sanko.fmap;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.overlayutil.OverlayManager;
import com.baidu.mapapi.overlayutil.PoiOverlay;
import com.baidu.mapapi.search.core.CityInfo;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchOption;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.sug.SuggestionSearch;


public class MainActivity extends Activity implements OnGetPoiSearchResultListener {
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
	private PoiSearch mPoiSearch = null;
	private BDLocation loc = null;
	private SuggestionSearch mSuggestionSearch = null;
	/**
	 * �����ؼ������봰��
	 */
	private AutoCompleteTextView keyWorldsView = null;
	private ArrayAdapter<String> sugAdapter = null;
	private int load_Index = 0;
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
        }else if(id==R.id.findbank){
        	findbank();
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
    		loc = location;
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
		
	if (isFirstLoc) {
		isFirstLoc = false;
		center2myLoc();
	}
	
	
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
    	mBaiduMap.clear();
		
    	try {
    		MapStatus mMapStatus = new MapStatus.Builder()
            .target(ll)
            .zoom(18.4f)
            .build();
            //����MapStatusUpdate�����Ա�������ͼ״̬��Ҫ�����ı仯
    	    MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
    	    //�ı��ͼ״̬
    	    mBaiduMap.animateMapStatus(mMapStatusUpdate);
    	    re.setText("��ǰλ�ã�"+loc.getAddrStr());
		} catch (NumberFormatException e) {
			Toast.makeText(this, "����", Toast.LENGTH_SHORT).show();
		}
    }
    
    private void findbank() {
    	//POI
    	LatLng ll = new LatLng(mCurrentLantitude, mCurrentLongitude);
    			mPoiSearch = PoiSearch.newInstance();
    			mPoiSearch.setOnGetPoiSearchResultListener(this);
    			mPoiSearch.searchNearby(new PoiNearbySearchOption()
    			.pageCapacity(50)
    			.location(ll)
    			.keyword("����")
    			.pageNum(0)
    			.radius(2000));
    			
	}

	@Override
	public void onGetPoiDetailResult(PoiDetailResult result) {
		// TODO Auto-generated method stub
		if (result.error != SearchResult.ERRORNO.NO_ERROR) {
			Toast.makeText(MainActivity.this, "��Ǹ��δ�ҵ����", Toast.LENGTH_SHORT)
					.show();
		} else {
			re.setText(result.getName());
		}
	}

	@Override
	public void onGetPoiResult(PoiResult result) {
		// TODO Auto-generated method stub
		if (result == null
				|| result.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
			Toast.makeText(MainActivity.this, "δ�ҵ����", Toast.LENGTH_LONG)
			.show();
			return;
		}
		if (result.error == SearchResult.ERRORNO.NO_ERROR) {
			mBaiduMap.clear();
			MyPoiOverlay overlay = new MyPoiOverlay(mBaiduMap);
			mBaiduMap.setOnMarkerClickListener(overlay);
			overlay.setData(result);
			overlay.addToMap();
			overlay.zoomToSpan();
			re.setText("��������"+result.getTotalPoiNum()+"������");
			return;
		}
		if (result.error == SearchResult.ERRORNO.AMBIGUOUS_KEYWORD) {

			// ������ؼ����ڱ���û���ҵ����������������ҵ�ʱ�����ذ����ùؼ�����Ϣ�ĳ����б�
			String strInfo = "��";
			for (CityInfo cityInfo : result.getSuggestCityList()) {
				strInfo += cityInfo.city;
				strInfo += ",";
			}
			strInfo += "�ҵ����";
			Toast.makeText(MainActivity.this, strInfo, Toast.LENGTH_LONG)
					.show();
		}
	}
	
//	private class MyPoiOverlay extends PoiOverlay {
//
//		public MyPoiOverlay(BaiduMap baiduMap) {
//			super(baiduMap);
//		}
//
//		@Override
//		public boolean onPoiClick(int index) {
//			super.onPoiClick(index);
//			PoiInfo poi = getPoiResult().getAllPoi().get(index);
//			// if (poi.hasCaterDetails) {
//				mPoiSearch.searchPoiDetail((new PoiDetailSearchOption())
//						.poiUid(poi.uid));
//			// }
//			return true;
//		}
//	}
//	
    /**
* ������
*/
private class MyPoiOverlay extends OverlayManager {
	private PoiResult poiResult = null;

	public MyPoiOverlay(BaiduMap baiduMap) {
		super(baiduMap);
	}

	public void setData(PoiResult poiResult) {
		this.poiResult = poiResult;
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		if (marker.getExtraInfo() != null) {
			int index = marker.getExtraInfo().getInt("index");
			PoiInfo poi = poiResult.getAllPoi().get(index);

			// ��������
			mPoiSearch.searchPoiDetail((new PoiDetailSearchOption())
					.poiUid(poi.uid));
			return true;
		}
		return false;
	}

	@Override
	public List<OverlayOptions> getOverlayOptions() {
		if ((this.poiResult == null)
				|| (this.poiResult.getAllPoi() == null))
			return null;
		ArrayList<OverlayOptions> arrayList = new ArrayList<OverlayOptions>();
		for (int i = 0; i < this.poiResult.getAllPoi().size(); i++) {
			if (this.poiResult.getAllPoi().get(i).location == null)
				continue;
			// ��marker���ϱ�ǩ
			Bundle bundle = new Bundle();
			bundle.putInt("index", i);
			arrayList.add(new MarkerOptions()
					.icon(BitmapDescriptorFactory
							.fromBitmap(setNumToIcon(i+1))).extraInfo(bundle)
					.position(this.poiResult.getAllPoi().get(i).location));
		}
		return arrayList;
	}

	/**
	 * ��ͼƬ�������
	 */
	private Bitmap setNumToIcon(int num) {
		BitmapDrawable bd = (BitmapDrawable) getResources().getDrawable(
				R.drawable.icon_gcoding);
		Bitmap bitmap = bd.getBitmap().copy(Bitmap.Config.ARGB_8888, true);
		Canvas canvas = new Canvas(bitmap);

		Paint paint = new Paint();
		paint.setColor(Color.WHITE);
		paint.setAntiAlias(true);
		int widthX;
		int heightY = 0;
		if (num < 10) {
			paint.setTextSize(30);
			widthX = 8;
			heightY = 6;
		} else {
			paint.setTextSize(20);
			widthX = 11;
		}

		canvas.drawText(String.valueOf(num),
				((bitmap.getWidth() / 2) - widthX),
				((bitmap.getHeight() / 2) + heightY), paint);
		return bitmap;
	}

}
}
