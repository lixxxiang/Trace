package com.example.lixiang.trace3.view

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.view.View
import android.widget.ImageView

import com.baidu.trace.LBSTraceClient
import com.baidu.trace.Trace
import com.baidu.trace.model.OnTraceListener
import com.baidu.trace.model.PushMessage
import com.baidu.trace.api.track.HistoryTrackRequest
import com.baidu.trace.api.track.HistoryTrackResponse
import com.baidu.trace.api.track.OnTrackListener
import com.example.lixiang.trace3.R
import com.example.lixiang.trace3.util.DeviceUtils
import com.example.lixiang.trace3.util.Logger
import kotlinx.android.synthetic.main.activity_main.*
import android.widget.ZoomControls
import com.baidu.location.*
import com.baidu.mapapi.SDKInitializer
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import com.example.lixiang.trace3.R.id.end
import com.example.lixiang.trace3.R.string.start
import com.example.lixiang.trace3.util.SpannableBuilder


class MainActivity : AppCompatActivity(), SensorEventListener {

    internal var serviceId: Long = 162046
    internal var entityName = ""
    internal var isNeedObjectStorage = false
    var mTrace = Trace()
    lateinit var mTraceClient: LBSTraceClient
    var tag = 1
    var mMapView: MapView? = null
    private var lastX: Double? = 0.0
    private var mCurrentDirection = 0
    private var mCurrentLat = 0.0
    private var mCurrentLon = 0.0
    private var mCurrentAccracy: Float = 0.toFloat()
    var myListener = MyLocationListenner()
    private var mLocClient: LocationClient? = null
    private var mBaiduMap: BaiduMap? = null
    private var isFirstLoc = true
    private var locData: MyLocationData? = null
    private var mSensorManager: SensorManager? = null


    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }

    internal var mTraceListener: OnTraceListener = object : OnTraceListener {
        override fun onBindServiceCallback(i: Int, s: String) {

        }

        override fun onStartTraceCallback(status: Int, message: String) {
            println(String.format("onStartTraceCallback, errorNo:%d, message:%s ", status, message))
        }

        override fun onStopTraceCallback(status: Int, message: String) {}

        override fun onStartGatherCallback(status: Int, message: String) {
            println(String.format("onStartGatherCallback, errorNo:%d, message:%s ", status, message))

        }

        override fun onStopGatherCallback(status: Int, message: String) {}

        override fun onPushCallback(messageNo: Byte, message: PushMessage) {}

        override fun onInitBOSCallback(i: Int, s: String) {

        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SDKInitializer.initialize(applicationContext)
        setContentView(R.layout.activity_main)
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        handlePermisson()
        initMap()
        btn.setOnClickListener{
            val historyTrackRequest = HistoryTrackRequest(tag, serviceId, entityName)

            val startTime = System.currentTimeMillis() / 1000 - 12 * 60 * 60
            val endTime = System.currentTimeMillis() / 1000
            historyTrackRequest.startTime = startTime
            historyTrackRequest.endTime = endTime

            val mTrackListener = object : OnTrackListener() {
                // 历史轨迹回调
                override fun onHistoryTrackCallback(response: HistoryTrackResponse?) {
                    Logger().d(response!!)
                }
            }

            mTraceClient.queryHistoryTrack(historyTrackRequest, mTrackListener)
        }
        getTime()

        tv1.text = SpannableBuilder.create(this)
                .append("距离上班时间还有 ",R.dimen.text_size, R.color.gray)
                .append("ooo", R.dimen.text_size, R.color.red)
                .build()
        start_rl.setOnClickListener {
            start_rl.visibility = View.INVISIBLE
            end_rl.visibility = View.VISIBLE
        }
        end_rl.setOnClickListener {
            start_rl.visibility = View.VISIBLE
            end_rl.visibility = View.INVISIBLE
        }
    }

    private fun getTime() {

    }

    private fun initMap() {
        val mCurrentMode = MyLocationConfiguration.LocationMode.FOLLOWING
        mMapView = findViewById<MapView>(R.id.mMapView)
        mBaiduMap = mMapView!!.map
        mBaiduMap!!.isMyLocationEnabled = true
        mBaiduMap!!.setMyLocationConfigeration(MyLocationConfiguration(mCurrentMode, true, null))
        val builder = MapStatus.Builder()
        builder.overlook(0f)
        mBaiduMap!!.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()))
        val child = mMapView!!.getChildAt(1)
        if (child != null && (child is ImageView || child is ZoomControls)) {
            child.visibility = View.INVISIBLE
        }

        mMapView!!.showScaleControl(false)
        mMapView!!.showZoomControls(false)
        val mUiSettings = mBaiduMap!!.uiSettings
        mUiSettings.isScrollGesturesEnabled = true
        mUiSettings.isOverlookingGesturesEnabled = true
        mUiSettings.isZoomGesturesEnabled = true
        mLocClient = LocationClient(this)
        mLocClient!!.registerLocationListener(myListener)
        val option = LocationClientOption()
        option.isOpenGps = true
        option.setCoorType("bd09ll")
        option.setScanSpan(1000)
        option.setAddrType("all")
        option.setIsNeedLocationPoiList(true)
        mLocClient!!.locOption = option
        mLocClient!!.start()
    }


    @RequiresApi(Build.VERSION_CODES.M)
    fun handlePermisson() {
        val permission = Manifest.permission.CAMERA
        val checkSelfPermission = ActivityCompat.checkSelfPermission(this, permission)
        if (checkSelfPermission == PackageManager.PERMISSION_GRANTED) {
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            } else {
                myRequestPermission()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun myRequestPermission() {
        val permissions = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
        requestPermissions(permissions, 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            val gatherInterval = 5
            val packInterval = 10
            entityName = DeviceUtils.getUniqueId(this)
            mTrace = Trace(serviceId, entityName, isNeedObjectStorage)
            mTraceClient = LBSTraceClient(applicationContext)
            mTraceClient.setInterval(gatherInterval, packInterval)
            mTraceClient.startTrace(mTrace, mTraceListener)
            mTraceClient.startGather(mTraceListener)


        }
    }


    inner class MyLocationListenner : BDLocationListener {
        var lati: Double = 0.toDouble()
        var longi: Double = 0.toDouble()
        var address: String = ""
        internal lateinit var poi: List<Poi>

        override fun onReceiveLocation(location: BDLocation?) {
            if (location == null || mMapView == null) {
                return
            }

            val locData = MyLocationData.Builder()
                    .accuracy(0F)
                    .direction(mCurrentDirection.toFloat())
                    .latitude(location.latitude)
                    .longitude(location.longitude).build()
            lati = location.latitude
            longi = location.longitude
            mCurrentLat = location.latitude
            mCurrentLon = location.longitude
            address = location.addrStr
            mCurrentAccracy = location.radius
            poi = location.poiList
            mBaiduMap!!.setMyLocationData(locData)
            if (isFirstLoc) {
                isFirstLoc = false
                val ll = LatLng(location.latitude,
                        location.longitude)
                val builder = MapStatus.Builder()
                builder.target(ll).zoom(18.0f)
                mBaiduMap!!.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()))
            }
        }

        fun onConnectHotSpotMessage(s: String, i: Int) {

        }

    }

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        val x = sensorEvent.values[SensorManager.DATA_X].toDouble()
        if (Math.abs(x - lastX!!) > 1.0) {
            mCurrentDirection = x.toInt()
            locData = MyLocationData.Builder()
                    .accuracy(0F)
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(mCurrentDirection.toFloat()).latitude(mCurrentLat)
                    .longitude(mCurrentLon).build()
            mBaiduMap!!.setMyLocationData(locData)
        }
        lastX = x

    }

    override fun onPause() {
        mMapView!!.onPause()
        super.onPause()
    }

    override fun onResume() {
        mMapView!!.onResume()
        super.onResume()
        mSensorManager!!.registerListener(this, mSensorManager!!.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_UI)
    }

    override fun onDestroy() {
        mTraceClient.stopTrace(mTrace, mTraceListener)
        mTraceClient.stopGather(mTraceListener)
        mLocClient!!.stop()
        mBaiduMap!!.isMyLocationEnabled = false
        mMapView!!.onDestroy()
        mMapView = null
        mSensorManager!!.unregisterListener(this)
        super.onDestroy()
    }
}
