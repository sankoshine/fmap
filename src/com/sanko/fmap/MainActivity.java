package com.sanko.fmap;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
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
import com.baidu.mapapi.map.BaiduMap.OnMapClickListener;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.overlayutil.DrivingRouteOverlay;
import com.baidu.mapapi.overlayutil.OverlayManager;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchOption;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.route.DrivingRouteLine;
import com.baidu.mapapi.search.route.DrivingRouteLine.DrivingStep;
import com.baidu.mapapi.search.route.DrivingRoutePlanOption;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteResult;


public class MainActivity extends Activity 
implements OnGetPoiSearchResultListener, OnGetRoutePlanResultListener {
	
	//UI
	MapView mMapView = null;
	TextView re = null;
	
	//field
	BaiduMap mBaiduMap = null;
	private BDLocation loc = null;//��ǰλ��
	int mode = 0;//��ǰ���ܣ���ֹ����������
	
	//locate
	LocationClient mLocationClient = null;
	boolean isFirstLoc = true;
	
	//poi
	private PoiSearch mPoiSearch = null;
	BitmapDescriptor mCurrentMarker = null;
	
	//route
	RoutePlanSearch mSearch = null;
    DrivingRouteLine route = null;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //��ʼ��Ҫ��setContentView����֮ǰʵ��  
        SDKInitializer.initialize(getApplicationContext());  
        setContentView(R.layout.main);
        
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        re = (TextView)findViewById(R.id.result_appear);
        
        //��λ����ǰλ��
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(new MyLocationListener());        
        LocationClientOption option = new LocationClientOption();
		option.setLocationMode(LocationMode.Hight_Accuracy);
		option.setCoorType("bd09ll");
		option.setScanSpan(1000);
		option.setIsNeedAddress(true);
		mLocationClient.setLocOption(option);
		mLocationClient.start();		
		//��λͼ�㳣��
		mBaiduMap.setMyLocationEnabled(true);
		
		//�ܱ�������ʼ��
		mPoiSearch = PoiSearch.newInstance();
    	mPoiSearch.setOnGetPoiSearchResultListener(this);
    	
		//·�߹滮��ʼ��
        mSearch = RoutePlanSearch.newInstance();
        mSearch.setOnGetRoutePlanResultListener(this);
		//�����ͼ�հ״������ʾ������route
        mBaiduMap.setOnMapClickListener(new OnMapClickListener() {			
			@Override
			public boolean onMapPoiClick(MapPoi arg0) {
				return false;
			}			
			@Override
			public void onMapClick(LatLng arg0) {
				mBaiduMap.hideInfoWindow();
			}
		});   
	}
    
    @Override  
    protected void onDestroy() {  
        super.onDestroy();
        mLocationClient.stop();
        mMapView.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.curplace) {
        	mode = 0;
        	center2myLoc();
            return true;
        }else if(id == R.id.findbank){
        	mode = 1;
        	findbank();
        	return true;
        }else if(id == R.id.gtstation){
        	mode = 2;
        	waytostation();
        	return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //XXX ==== locate ====
    
    private void center2myLoc() {
    	LatLng ll = new LatLng(loc.getLatitude(), loc.getLongitude());
    	mBaiduMap.clear();
    	//���в��Ŵ�
    	MapStatus mMapStatus = new MapStatus.Builder()
    	.target(ll)
    	.zoom(18.4f)
    	.build();
    	MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
    	mBaiduMap.animateMapStatus(mMapStatusUpdate);
    	re.setText("��ǰλ�ã�"+loc.getAddrStr());		
    }
   
    public class MyLocationListener implements BDLocationListener {
    	@Override
    	public void onReceiveLocation(BDLocation location) {
    		if (location == null)return ;
    		
    		MyLocationData locData = new MyLocationData.Builder()
    		.accuracy(location.getRadius())
			.latitude(location.getLatitude())
			.longitude(location.getLongitude()).build();
    		mBaiduMap.setMyLocationData(locData);
    		
    		//��ֵ��ȫ�ֱ���
    		loc = location;     		
    		//��Ӧ��ʱֱ����ʾ��ǰλ��
    		if (isFirstLoc){
    			isFirstLoc = false;
    			center2myLoc();
    		}	
    	}
    }
    
    //XXX ==== poi ====
    
    private void findbank() {
    	LatLng ll = new LatLng(loc.getLatitude(), loc.getLongitude());
    	mPoiSearch.searchNearby(new PoiNearbySearchOption()
    	.pageCapacity(50)//�����ʾ50��
    	.location(ll)
    	.keyword("����")
    	.pageNum(0)//��һҳ
    	.radius(2000));//�����뾶��2000m 			
	}

	@Override
	public void onGetPoiDetailResult(PoiDetailResult result) {
		if (mode != 1) return;
		//������������ʾ��������
		re.setText(result.getName());
	}

	@Override
	public void onGetPoiResult(PoiResult result) {
		if (mode != 1) return;
		if (result.error != SearchResult.ERRORNO.NO_ERROR) {
			Toast.makeText(MainActivity.this, "����û������", Toast.LENGTH_LONG)
			.show();
			return;
		}
		mBaiduMap.clear();
		//��������ʾ
		MyPoiOverlay overlay = new MyPoiOverlay(mBaiduMap);
		mBaiduMap.setOnMarkerClickListener(overlay);
		overlay.setData(result);
		overlay.addToMap();
		overlay.zoomToSpan();
		re.setText("��������"+result.getTotalPoiNum()+"������");
		return;
	}
	
	//�������Ҫ��ʾ10�����Ͻ����ֱ�� extends PoiOverlay �����������鼴��
	private class MyPoiOverlay extends OverlayManager {
		private PoiResult poiResult = null;
		public MyPoiOverlay(BaiduMap baiduMap) {
			super(baiduMap);
		}
		public void setData(PoiResult poiResult) {
			this.poiResult = poiResult;
		}
		
		//����������ʾ����
		@Override
		public boolean onMarkerClick(Marker marker) {
			if (marker.getExtraInfo() != null) {
				int index = marker.getExtraInfo().getInt("index");
				PoiInfo poi = poiResult.getAllPoi().get(index);
				mPoiSearch.searchPoiDetail((new PoiDetailSearchOption())
						.poiUid(poi.uid));
				return true;
			}
			return false;
		}
		
		//������ֺͱ�ǩ
		@Override
		public List<OverlayOptions> getOverlayOptions() {
			if ((this.poiResult == null)||(this.poiResult.getAllPoi() == null))
				return null;
			ArrayList<OverlayOptions> arrayList = new ArrayList<OverlayOptions>();
			for (int i = 0; i < this.poiResult.getAllPoi().size(); i++) {
				if (this.poiResult.getAllPoi().get(i).location == null)
					continue;
				//��marker���ϱ�ǩ
				Bundle bundle = new Bundle();
				bundle.putInt("index", i);
				arrayList.add(new MarkerOptions()
				.icon(BitmapDescriptorFactory
						.fromBitmap(setNumToIcon(i+1))).extraInfo(bundle)
						.position(this.poiResult.getAllPoi().get(i).location));
				}
			return arrayList;
		}
		
		//��ͼƬ�������
		private Bitmap setNumToIcon(int num) {
			BitmapDrawable bd = (BitmapDrawable)getResources().getDrawable(
					R.drawable.icon_gcoding);
			Bitmap bitmap = bd.getBitmap().copy(Bitmap.Config.ARGB_8888, true);
			Canvas canvas = new Canvas(bitmap);
			Paint paint = new Paint();
			paint.setColor(Color.WHITE);
			paint.setAntiAlias(true);
			int widthX;
			int heightY = 0;
			if (num<10) {
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
    
    //XXX ==== route ====
    
	private void waytostation() {
		mBaiduMap.clear();
		LatLng ll = new LatLng(loc.getLatitude(), loc.getLongitude());
		//��㣺��ǰλ�ã��յ㣺���ػ�վ
        PlanNode stNode = PlanNode.withLocation(ll);
        PlanNode enNode = PlanNode.withCityNameAndPlaceName(loc.getCity(), "��վ");
        mSearch.drivingSearch((new DrivingRoutePlanOption())
                .from(stNode)
                .to(enNode));        
	}
	
	//���������ڵ���վ����������ʱ��ѡ���һ������Ϊ�ص�
	private void waytostation(LatLng l) {
		mBaiduMap.clear();
		LatLng ll = new LatLng(loc.getLatitude(), loc.getLongitude());
        PlanNode stNode = PlanNode.withLocation(ll);
        PlanNode enNode = PlanNode.withLocation(l);
        mSearch.drivingSearch((new DrivingRoutePlanOption())
                .from(stNode)
                .to(enNode));        
	}
	
	@Override
	public void onGetDrivingRouteResult(DrivingRouteResult result) {
		if (mode != 2) return;
		if (result == null) {
            Toast.makeText(MainActivity.this, "�޷�����", Toast.LENGTH_SHORT).show();
            return;
        }
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            //�յ㲻Ψһʱ��ȡ��һ�����
        	List<PoiInfo> ls = result.getSuggestAddrInfo().getSuggestEndNode();
        	waytostation(ls.get(0).location);
        	return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {
        	route = result.getRouteLines().get(0);
            DrivingRouteOverlay overlay = new MyDrivingRouteOverlay(mBaiduMap);
            mBaiduMap.setOnMarkerClickListener(overlay);
            overlay.setData(route);
            overlay.addToMap();
            overlay.zoomToSpan();
            re.setText("���������ǰ��"+loc.getCity()+"��վ");         
        }
	}

	//�����Ͳ��в�ʹ��
	@Override
	public void onGetTransitRouteResult(TransitRouteResult arg0) {		
	}
	@Override
	public void onGetWalkingRouteResult(WalkingRouteResult arg0) {		
	}
	
	//���������ʾ
    private class MyDrivingRouteOverlay extends DrivingRouteOverlay {
        public MyDrivingRouteOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }
        @Override
        public boolean onRouteNodeClick(int index){
        	DrivingStep step = route.getAllStep().get(index);            
            LatLng nodeLocation = step.getEntrace().getLocation();
            String nodeTitle = step.getInstructions();
            if (nodeLocation == null || nodeTitle == null) {
                return false;
            }
            //�ƶ�����Ұ����
            mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(nodeLocation));
            //������ʾ
            TextView popupText = new TextView(MainActivity.this);
            popupText.setBackgroundColor(Color.RED);
            popupText.setTextColor(Color.BLUE);
            popupText.setText(nodeTitle);
            mBaiduMap.showInfoWindow(new InfoWindow(popupText, nodeLocation, 50));
			return true;        	
        }      
    }
}
