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
import com.baidu.mapapi.search.core.CityInfo;
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


public class MainActivity extends Activity implements OnGetPoiSearchResultListener, 
OnMapClickListener, OnGetRoutePlanResultListener {
	//UI：上方为textview，下方为mapview
	MapView mMapView = null;
	TextView re = null;
	public LocationClient mLocationClient = null;
	public BDLocationListener myListener = new MyLocationListener();
	BaiduMap mBaiduMap = null;
	LocationClient mLocClient;
	BitmapDescriptor mCurrentMarker;
	boolean isFirstLoc = true;
	public MyLocationListener mMyLocationListener;
	private PoiSearch mPoiSearch = null;
	private BDLocation loc = null;
    RoutePlanSearch mSearch = null;  
    OverlayManager routeOverlay = null;
    DrivingRouteLine route = null;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        //注意该方法要再setContentView方法之前实现  
        SDKInitializer.initialize(getApplicationContext());  
        setContentView(R.layout.main);
        
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        re = (TextView)findViewById(R.id.result_appear);
        mLocationClient = new LocationClient(getApplicationContext());     //声明LocationClient类
        mLocationClient.registerLocationListener( myListener ); 
        LocationClientOption option = new LocationClientOption();
		option.setLocationMode(LocationMode.Hight_Accuracy);//设置定位模式
		option.setCoorType("bd09ll");//返回的定位结果是百度经纬度，默认值gcj02
		int span=1000;
		option.setScanSpan(span);//设置发起定位请求的间隔时间为5000ms
		option.setIsNeedAddress(true);
		mLocationClient.setLocOption(option);
		mLocationClient.start();
		mLocationClient.requestLocation();
		
		//地图点击事件处理
        mBaiduMap.setOnMapClickListener(this);
        // 初始化搜索模块，注册事件监听
        mSearch = RoutePlanSearch.newInstance();
        mSearch.setOnGetRoutePlanResultListener(this);
				
				
    
	}
    
    @Override  
    protected void onDestroy() {  
        super.onDestroy();  
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理  
    
        mMapView.onDestroy(); 
    }  
    @Override  
    protected void onResume() {  
        super.onResume();  
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理  
        mMapView.onResume();  
     // 开启定位图层
		mBaiduMap.setMyLocationEnabled(true);
        }  
    @Override  
    protected void onPause() {  
        super.onPause();  
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理  
        mMapView.onPause();  
        // 当不需要定位图层时关闭定位图层  
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
        if (id == R.id.curplace) {
        	center2myLoc();
            return true;
        }else if(id==R.id.findbank){
        	findbank();
        	return true;
        }else if(id==R.id.gtstation){
        	waytostation();
        	return true;
        }
        return super.onOptionsItemSelected(item);
    }

	private void waytostation() {
		 //设置起终点信息
		mBaiduMap.clear();
		LatLng ll = new LatLng(loc.getLatitude(), loc.getLongitude());
        PlanNode stNode = PlanNode.withLocation(ll);
        PlanNode enNode = PlanNode.withCityNameAndPlaceName(loc.getCity(), "火车站");
        mSearch.drivingSearch((new DrivingRoutePlanOption())
                .from(stNode)
                .to(enNode));        
	}
	
	private void waytostation(LatLng l) {
		 //设置起终点信息
		mBaiduMap.clear();
		LatLng ll = new LatLng(loc.getLatitude(), loc.getLongitude());
        PlanNode stNode = PlanNode.withLocation(ll);
        PlanNode enNode = PlanNode.withLocation(l);
        mSearch.drivingSearch((new DrivingRoutePlanOption())
                .from(stNode)
                .to(enNode));        
	}

	public class MyLocationListener implements BDLocationListener {
    	
    	TextView mLocationResult;
    	
    	@Override
    	public void onReceiveLocation(BDLocation location) {
    		if (location == null)
    	            return ;
    		loc = location;  		

    		MyLocationData locData = new MyLocationData.Builder()
    		.accuracy(location.getRadius())
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
    	LatLng ll = new LatLng(loc.getLatitude(), loc.getLongitude());
    	mBaiduMap.clear();
		
    	try {
    		MapStatus mMapStatus = new MapStatus.Builder()
            .target(ll)
            .zoom(18.4f)
            .build();
            //定义MapStatusUpdate对象，以便描述地图状态将要发生的变化
    	    MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
    	    //改变地图状态
    	    mBaiduMap.animateMapStatus(mMapStatusUpdate);
    	    re.setText("当前位置："+loc.getAddrStr());
		} catch (NumberFormatException e) {
			Toast.makeText(this, "定位到当前位置发生错误", Toast.LENGTH_SHORT).show();
		}
    }
    
    private void findbank() {
    	//POI
    	LatLng ll = new LatLng(loc.getLatitude(), loc.getLongitude());
    			mPoiSearch = PoiSearch.newInstance();
    			mPoiSearch.setOnGetPoiSearchResultListener(this);
    			mPoiSearch.searchNearby(new PoiNearbySearchOption()
    			.pageCapacity(50)
    			.location(ll)
    			.keyword("银行")
    			.pageNum(0)
    			.radius(2000));
    			
	}

	@Override
	public void onGetPoiDetailResult(PoiDetailResult result) {
		if (result.error != SearchResult.ERRORNO.NO_ERROR) {
			Toast.makeText(MainActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT)
					.show();
		} else {
			re.setText(result.getName());
		}
	}

	@Override
	public void onGetPoiResult(PoiResult result) {
		if (result == null
				|| result.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
			Toast.makeText(MainActivity.this, "未找到结果", Toast.LENGTH_LONG)
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
			re.setText("共搜索到"+result.getTotalPoiNum()+"家银行");
			return;
		}
		if (result.error == SearchResult.ERRORNO.AMBIGUOUS_KEYWORD) {

			// 当输入关键字在本市没有找到，但在其他城市找到时，返回包含该关键字信息的城市列表
			String strInfo = "在";
			for (CityInfo cityInfo : result.getSuggestCityList()) {
				strInfo += cityInfo.city;
				strInfo += ",";
			}
			strInfo += "找到结果";
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
* 覆盖物
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

			// 详情搜索
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
			// 给marker加上标签
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
	 * 往图片添加数字
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

	@Override
	public void onGetDrivingRouteResult(DrivingRouteResult result) {
		if (result == null) {
            Toast.makeText(MainActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
            return;
        }
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            //起终点或途经点地址有岐义，通过以下接口获取建议查询信息
        	List<PoiInfo> ls = result.getSuggestAddrInfo().getSuggestEndNode();
        	waytostation(ls.get(0).location);
        	return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {
           route = result.getRouteLines().get(0);
            DrivingRouteOverlay overlay = new MyDrivingRouteOverlay(mBaiduMap);
            routeOverlay = overlay;
            mBaiduMap.setOnMarkerClickListener(overlay);
            overlay.setData(result.getRouteLines().get(0));
            overlay.addToMap();
            overlay.zoomToSpan();
            re.setText("从这里出发前往"+loc.getCity()+"火车站");
         
        }
            
            
       
	}

	@Override
	public void onGetTransitRouteResult(TransitRouteResult arg0) {		
	}

	@Override
	public void onGetWalkingRouteResult(WalkingRouteResult arg0) {		
	}

	@Override
	public void onMapClick(LatLng arg0) {
		mBaiduMap.hideInfoWindow();
	}

	@Override
	public boolean onMapPoiClick(MapPoi arg0) {
		return false;
	}
	
	//定制RouteOverly
    private class MyDrivingRouteOverlay extends DrivingRouteOverlay {

        public MyDrivingRouteOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public boolean onRouteNodeClick(int index){
        	
        	 //获取节结果信息
            LatLng nodeLocation = null;
            String nodeTitle = null;
            DrivingStep step = route.getAllStep().get(index);
            
                nodeLocation = step.getEntrace().getLocation();
                nodeTitle = step.getInstructions();
           

            if (nodeLocation == null || nodeTitle == null) {
                return false;
            }
        	
        //	移动节点至中心
          mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(nodeLocation));
          // show popup
          TextView popupText = new TextView(MainActivity.this);
          popupText.setBackgroundColor(Color.RED);
          popupText.setTextColor(Color.BLUE);
          popupText.setText(nodeTitle);
          mBaiduMap.showInfoWindow(new InfoWindow(popupText, nodeLocation, 0));
			return true;
        	
        }
      
    }
}
