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
	private BDLocation loc = null;//当前位置
	int mode = 0;//当前功能，防止监听器乱跳
	
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
        //初始化要在setContentView方法之前实现  
        SDKInitializer.initialize(getApplicationContext());  
        setContentView(R.layout.main);
        
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        re = (TextView)findViewById(R.id.result_appear);
        
        //定位出当前位置
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(new MyLocationListener());        
        LocationClientOption option = new LocationClientOption();
		option.setLocationMode(LocationMode.Hight_Accuracy);
		option.setCoorType("bd09ll");
		option.setScanSpan(1000);
		option.setIsNeedAddress(true);
		mLocationClient.setLocOption(option);
		mLocationClient.start();		
		//定位图层常开
		mBaiduMap.setMyLocationEnabled(true);
		
		//周边搜索初始化
		mPoiSearch = PoiSearch.newInstance();
    	mPoiSearch.setOnGetPoiSearchResultListener(this);
    	
		//路线规划初始化
        mSearch = RoutePlanSearch.newInstance();
        mSearch.setOnGetRoutePlanResultListener(this);
		//点击地图空白处清除提示，用于route
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
    	//居中并放大
    	MapStatus mMapStatus = new MapStatus.Builder()
    	.target(ll)
    	.zoom(18.4f)
    	.build();
    	MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
    	mBaiduMap.animateMapStatus(mMapStatusUpdate);
    	re.setText("当前位置："+loc.getAddrStr());		
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
    		
    		//赋值给全局变量
    		loc = location;     		
    		//打开应用时直接显示当前位置
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
    	.pageCapacity(50)//最多显示50个
    	.location(ll)
    	.keyword("银行")
    	.pageNum(0)//第一页
    	.radius(2000));//搜索半径：2000m 			
	}

	@Override
	public void onGetPoiDetailResult(PoiDetailResult result) {
		if (mode != 1) return;
		//点击搜索结果显示银行名称
		re.setText(result.getName());
	}

	@Override
	public void onGetPoiResult(PoiResult result) {
		if (mode != 1) return;
		if (result.error != SearchResult.ERRORNO.NO_ERROR) {
			Toast.makeText(MainActivity.this, "附近没有银行", Toast.LENGTH_LONG)
			.show();
			return;
		}
		mBaiduMap.clear();
		//搜索并显示
		MyPoiOverlay overlay = new MyPoiOverlay(mBaiduMap);
		mBaiduMap.setOnMarkerClickListener(overlay);
		overlay.setData(result);
		overlay.addToMap();
		overlay.zoomToSpan();
		re.setText("共搜索到"+result.getTotalPoiNum()+"家银行");
		return;
	}
	
	//如果不需要显示10个以上结果，直接 extends PoiOverlay 并处理点击详情即可
	private class MyPoiOverlay extends OverlayManager {
		private PoiResult poiResult = null;
		public MyPoiOverlay(BaiduMap baiduMap) {
			super(baiduMap);
		}
		public void setData(PoiResult poiResult) {
			this.poiResult = poiResult;
		}
		
		//处理点击，显示详情
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
		
		//添加数字和标签
		@Override
		public List<OverlayOptions> getOverlayOptions() {
			if ((this.poiResult == null)||(this.poiResult.getAllPoi() == null))
				return null;
			ArrayList<OverlayOptions> arrayList = new ArrayList<OverlayOptions>();
			for (int i = 0; i < this.poiResult.getAllPoi().size(); i++) {
				if (this.poiResult.getAllPoi().get(i).location == null)
					continue;
				//给marker加上标签
				Bundle bundle = new Bundle();
				bundle.putInt("index", i);
				arrayList.add(new MarkerOptions()
				.icon(BitmapDescriptorFactory
						.fromBitmap(setNumToIcon(i+1))).extraInfo(bundle)
						.position(this.poiResult.getAllPoi().get(i).location));
				}
			return arrayList;
		}
		
		//往图片添加数字
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
		//起点：当前位置，终点：当地火车站
        PlanNode stNode = PlanNode.withLocation(ll);
        PlanNode enNode = PlanNode.withCityNameAndPlaceName(loc.getCity(), "火车站");
        mSearch.drivingSearch((new DrivingRoutePlanOption())
                .from(stNode)
                .to(enNode));        
	}
	
	//该重载用于当火车站搜索有歧义时，选择第一项结果作为重点
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
            Toast.makeText(MainActivity.this, "无法到达", Toast.LENGTH_SHORT).show();
            return;
        }
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            //终点不唯一时，取第一个结果
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
            re.setText("从这里出发前往"+loc.getCity()+"火车站");         
        }
	}

	//公交和步行不使用
	@Override
	public void onGetTransitRouteResult(TransitRouteResult arg0) {		
	}
	@Override
	public void onGetWalkingRouteResult(WalkingRouteResult arg0) {		
	}
	
	//点击出现提示
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
            //移动到视野中心
            mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(nodeLocation));
            //弹出提示
            TextView popupText = new TextView(MainActivity.this);
            popupText.setBackgroundColor(Color.RED);
            popupText.setTextColor(Color.BLUE);
            popupText.setText(nodeTitle);
            mBaiduMap.showInfoWindow(new InfoWindow(popupText, nodeLocation, 50));
			return true;        	
        }      
    }
}
