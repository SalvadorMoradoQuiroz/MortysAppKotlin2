package com.teammortys.mortysappkotlin

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.*
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import com.harrysoft.androidbluetoothserial.BluetoothManager
import com.harrysoft.androidbluetoothserial.SimpleBluetoothDeviceInterface
import com.teammortys.mortysappkotlin.ObjectDetectorHelper.DetectorListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.tensorflow.lite.task.vision.detector.Detection
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.Charset
import java.util.*


class MainActivity : AppCompatActivity(), View.OnClickListener, DetectorListener,
    SimpleBluetoothDeviceInterface.OnMessageSentListener,
    SimpleBluetoothDeviceInterface.OnMessageReceivedListener,
    SimpleBluetoothDeviceInterface.OnErrorListener {

    companion object {
        private const val TAG = "MainActivity"
        private val MY_UUID_INSECURE: UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")
    }

    //BT
    var mBluetoothAdapter: BluetoothAdapter? = null
    var mBluetoothConnection: BluetoothConnectionService? = null
    var mBTDevice: BluetoothDevice? = null
    var mBTDevices: ArrayList<BluetoothDevice> = ArrayList<BluetoothDevice>()
    var mDeviceListAdapter: DeviceListAdapter? = null
    var lvNewDevices: ListView? = null

    //BT libBluetooth
    var deviceMAC: String? = null
    var deviceName: String? = null

    // A CompositeDisposable that keeps track of all of our asynchronous tasks
    private val compositeDisposable = CompositeDisposable()

    // Our BluetoothManager!
    private var bluetoothManager: BluetoothManager? = null

    // Our Bluetooth Device! When disconnected it is null, so make sure we know that we need to deal with it potentially being null
    @Nullable
    private var deviceInterface: SimpleBluetoothDeviceInterface? = null
    //

    //---------------------------------------------------------
    private var stream_thread: HandlerThread? = null
    private var flash_thread: HandlerThread? = null
    private var rssi_thread: HandlerThread? = null
    private var stream_handler: Handler? = null
    private var flash_handler: Handler? = null
    private var rssi_handler: Handler? = null

    private var ID_CONNECT: Int = 200
    private var ID_FLASH: Int = 201
    private var ID_RSSI: Int = 202

    private var obj_detect: Boolean = false
    private var flash_on_off: Boolean = false
    private var flagVideo = false

    private lateinit var objectDetectorHelper: ObjectDetectorHelper

    private lateinit var tracking_overlay: OverlayView
    private var imageMonitor: ImageView? = null
    private var button_Video: Button? = null
    private var switch_ObjDetect: SwitchMaterial? = null
    private var switch_Flash: SwitchMaterial? = null
    private var textView_RSSI: TextView? = null
    private var imageButton_ClawClose: ImageButton? = null
    private var imageButton_Up: ImageButton? = null
    private var imageButton_Down: ImageButton? = null
    private var imageButton_ClawOpen: ImageButton? = null
    private var imageButton_ZipperUp: ImageButton? = null
    private var imageButton_Right: ImageButton? = null
    private var imageButton_Left: ImageButton? = null
    private var imageButton_ZipperDown: ImageButton? = null
    //---------------------------------------------------------

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mBroadcastReceiver1)
        unregisterReceiver(mBroadcastReceiver2)
        unregisterReceiver(mBroadcastReceiver3)
        unregisterReceiver(mBroadcastReceiver4)
        //mBluetoothAdapter.cancelDiscovery()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        imageMonitor = findViewById(R.id.imageMonitor)
        tracking_overlay = findViewById(R.id.tracking_overlay)
        button_Video = findViewById(R.id.button_Video)
        switch_ObjDetect = findViewById(R.id.switch_ObjDetect)
        textView_RSSI = findViewById(R.id.textView_RSSI)
        imageButton_ClawClose = findViewById(R.id.imageButton_ClawClose)
        imageButton_Up = findViewById(R.id.imageButton_Up)
        imageButton_Down = findViewById(R.id.imageButton_Down)
        imageButton_ClawOpen = findViewById(R.id.imageButton_ClawOpen)
        imageButton_ZipperUp = findViewById(R.id.imageButton_ZipperUp)
        imageButton_Right = findViewById(R.id.imageButton_Right)
        imageButton_Left = findViewById(R.id.imageButton_Left)
        imageButton_ZipperDown = findViewById(R.id.imageButton_ZipperDown)
        switch_Flash = findViewById(R.id.switch_Flash)

        imageButton_ClawClose!!.setOnClickListener(this)
        imageButton_Up!!.setOnClickListener(this)
        imageButton_Down!!.setOnClickListener(this)
        imageButton_ClawOpen!!.setOnClickListener(this)
        imageButton_ZipperUp!!.setOnClickListener(this)
        imageButton_Right!!.setOnClickListener(this)
        imageButton_Left!!.setOnClickListener(this)
        imageButton_ZipperDown!!.setOnClickListener(this)
        button_Video!!.setOnClickListener(this)
        switch_Flash!!.setOnClickListener(this)
        switch_ObjDetect!!.setOnClickListener(this)

        stream_thread = HandlerThread("http")
        stream_thread!!.start()
        stream_handler = HttpHandler(this, stream_thread!!.looper)

        flash_thread = HandlerThread("http")
        flash_thread!!.start()
        flash_handler = HttpHandler(this, flash_thread!!.looper)

        rssi_thread = HandlerThread("http")
        rssi_thread!!.start()
        rssi_handler = HttpHandler(this, rssi_thread!!.looper)

        objectDetectorHelper = ObjectDetectorHelper(
            context = applicationContext,
            objectDetectorListener = this
        )

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // Setup our BluetoothManager
        bluetoothManager = BluetoothManager.instance
        //

        /*textViewObjectDetected = findViewById(R.id.textViewObjectDetected)

        //editTextIP!!.setText("192.168.43.180")

        checkBTPermissions()*/
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_opciones, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        when (item.getItemId()) {
            R.id.sw_bluewifi -> {}

            R.id.conf_bt -> showDialogConfBt()

            R.id.conf_conexion -> {}

            else -> {}
        }

        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("MissingPermission")
    override fun onClick(v: View) {
        when (v.id) {
            R.id.button_Video -> {
                if (!flagVideo) {
                    flagVideo = true
                    stream_handler!!.sendEmptyMessage(ID_CONNECT)
                    rssi_handler!!.sendEmptyMessage(ID_RSSI)
                    button_Video!!.setText("Video OFF")
                } else {
                    flagVideo = false
                    button_Video!!.setText("Video ON")
                }
            }
            R.id.switch_Flash -> {
                if (switch_Flash!!.isChecked) {
                    flash_on_off = true
                    flash_handler!!.sendEmptyMessage(ID_FLASH)
                } else {
                    flash_on_off = false
                    flash_handler!!.sendEmptyMessage(ID_FLASH)
                }
            }
            R.id.switch_ObjDetect -> {
                obj_detect = switch_ObjDetect!!.isChecked
            }
            R.id.imageButton_Up -> {
                Log.e("btn UP:", "Click")
                //enviarCaracter("a")
                deviceInterface!!.sendMessage("A")
            }
            else -> {}
        }
    }

    private inner class HttpHandler(private val context: Context, looper: Looper) :
        Handler(looper) {
        override fun handleMessage(@NonNull msg: Message) {
            super.handleMessage(msg)

            when (msg.what) {
                this@MainActivity.ID_CONNECT -> this@MainActivity.videoStream()
                this@MainActivity.ID_FLASH -> this@MainActivity.setFlash()
                this@MainActivity.ID_RSSI -> this@MainActivity.getRSSI()
                else -> {}
            }
        }

    }

    //Métodos para solicitar peticiones a ESP 32 CAM -----------------------------------------------
    private fun setFlash() {
        val flash_url: String
        if (flash_on_off) {
            flash_url = "http://192.168.43.160/onFlash"
            //buttonFlash!!.text = "FLASH OFF"
        } else {
            flash_url = "http://192.168.43.160/offFlash"
            //buttonFlash!!.text = "FLASH ON"
        }
        try {
            val url = URL(flash_url)
            val huc = url.openConnection() as HttpURLConnection
            huc.requestMethod = "GET"
            huc.connectTimeout = 1000 * 5
            huc.readTimeout = 1000 * 5
            huc.doInput = true
            huc.connect()
            if (huc.responseCode == 200) {
                val `in` = huc.inputStream
                val isr = InputStreamReader(`in`)
                val br = BufferedReader(isr)
                Log.e("Res", br.readLine())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getRSSI() {
        rssi_handler!!.sendEmptyMessageDelayed(ID_RSSI, 500)
        //val rssi_url = "http://" + editTextIP!!.text.toString().trim { it <= ' ' } + "/RSSI"
        val rssi_url = "http://192.168.43.160/RSSI"
        try {
            val url = URL(rssi_url)
            try {
                val huc = url.openConnection() as HttpURLConnection
                huc.requestMethod = "GET"
                huc.connectTimeout = 1000 * 5
                huc.readTimeout = 1000 * 5
                huc.doInput = true
                huc.connect()
                if (huc.responseCode == 200) {
                    val `in` = huc.inputStream
                    val isr = InputStreamReader(`in`)
                    val br = BufferedReader(isr)
                    val data = br.readLine()
                    if (!data.isEmpty()) {
                        runOnUiThread { textView_RSSI!!.text = data }
                    }
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        }
    }

    private fun videoStream() {
        //val url = "http://" + editTextIP!!.text.toString().trim { it <= ' ' } + ":81/cam-hi.jpg"
        val url = "http://192.168.43.160:81/cam-hi.jpg"
        while (flagVideo) {
            try {
                val `is` = URL(url).content as InputStream
                Log.e("Data", `is`.toString())

                //val d = Drawable.createFromStream(`is`, null)
                //runOnUiThread { imageMonitor!!.setImageDrawable(d) }

                val bitmap = BitmapFactory.decodeStream(`is`)
                runOnUiThread {
                    var mBitmapDebug = Bitmap.createScaledBitmap(
                        bitmap,
                        imageMonitor!!.width,
                        imageMonitor!!.height,
                        false
                    )
                    imageMonitor!!.setImageBitmap(mBitmapDebug)
                }
                if (obj_detect) {
                    objectDetectorHelper.detect(bitmap, 90)
                } else {
                    tracking_overlay.clear()
                }

            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
        runOnUiThread { imageMonitor!!.setImageResource(R.drawable.play_video) }
    }

    //Listeners Object detect-----------------------------------------------------------------------
    override fun onErrorObjDetector(error: String) {
        Toast.makeText(applicationContext, error, Toast.LENGTH_LONG).show()
    }

    override fun onResults(
        results: MutableList<Detection>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int,
    ) {
        if (results != null) {
            tracking_overlay.setResults(results, imageHeight, imageWidth)
            tracking_overlay.invalidate()
            /*for (result in results) {
                val textResult = result.categories[0].label + " " +
                        String.format("%.2f", result.categories[0].score)
                Log.e("Object detected:", textResult)
                textViewObjectDetected!!.setText(textResult)
            }*/
        }
    }


    //BLUETOOTH-------------------------------------------------------------------------------------
    //Método  para recibir de bt
    @SuppressLint("MissingPermission")
    fun showDialogConfBt() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.layout_config_bt, null)
        builder.setView(view)
        val dialogConfBt: AlertDialog = builder.create()
        dialogConfBt.setCancelable(false)
        dialogConfBt.show()

        var switch_BtActivate = dialogConfBt.findViewById(R.id.switch_BtActivated) as SwitchMaterial
        var button_VisibleBt = dialogConfBt.findViewById(R.id.button_VisibleBt) as Button
        var button_SearchDevicesBt =
            dialogConfBt.findViewById(R.id.button_SearchDevicesBt) as Button
        lvNewDevices = dialogConfBt.findViewById(R.id.listView_DevicesBt) as ListView
        var textView_DeviceSelected =
            dialogConfBt.findViewById(R.id.textView_DeviceSelected) as TextView
        var button_ConnectBt = dialogConfBt.findViewById(R.id.button_ConnectBt) as Button
        var button_CloseConfigBt = dialogConfBt.findViewById(R.id.button_CloseConfigBt) as Button

        if (mBluetoothAdapter!!.isEnabled) {
            switch_BtActivate.setText("Bluetooth activado")
            switch_BtActivate.isChecked = true
        }

        switch_BtActivate.setOnClickListener {
            //checkBTPermissions()
            enableDisableBT()
            /*if(mBluetoothAdapter!!.isEnabled){
                switch_BtActivate.setText("Bluetooth activado")
            }else{
                switch_BtActivate.setText("Bluetooth desactivado")
            }*/
        }

        button_VisibleBt.setOnClickListener({})

        button_SearchDevicesBt.setOnClickListener({ buscarBT() })

        lvNewDevices!!.setOnItemClickListener(
            AdapterView.OnItemClickListener({ parent, view, position, id ->
                mBluetoothAdapter?.cancelDiscovery()

                val deviceName: String = mBTDevices.get(position).getName()
                val deviceAddress: String = mBTDevices.get(position).getAddress()
                deviceMAC = deviceAddress
                this.deviceName = deviceName

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    mBTDevices.get(position).createBond()
                    mBTDevice = mBTDevices.get(position)
                    mBluetoothConnection = BluetoothConnectionService(this@MainActivity)
                }
                textView_DeviceSelected.setText(mBTDevices.get(position).toString())
            })
        )

        button_ConnectBt.setOnClickListener({
            //startConnection()
            compositeDisposable.add(bluetoothManager!!.openSerialDevice(deviceMAC!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ device -> onConnected(device.toSimpleDeviceInterface()) }) { t ->
                })
        })

        button_CloseConfigBt.setOnClickListener({ dialogConfBt.dismiss() })

    }

    // Called once the library connects a bluetooth device
    private fun onConnected(deviceInterface: SimpleBluetoothDeviceInterface) {
        this.deviceInterface = deviceInterface
        if (deviceInterface != null) {
            /*val putada = Objeto(applicationContext,this.deviceInterface!!)
            this.deviceInterface = putada.regreso()
            this.deviceInterface!!.sendMessage("Hola")*/
            this.deviceInterface!!.setListeners(this, this, this)
        } else {
            //deviceInterface was null, so the connection failed
        }

    }

    override fun onMessageSent(message: String) {
        // We sent a message! Handle it here.
        Toast.makeText(applicationContext, "Sent a message! Message was: $message", Toast.LENGTH_LONG) .show() // Replace context with your context instance.
    }

    override fun onMessageReceived(message: String) {
        // We received a message! Handle it here.
        Toast.makeText(applicationContext, "Received a message! Message was: $message", Toast.LENGTH_LONG).show() // Replace context with your context instance.
    }

    override fun onError(error: Throwable) {
        // Handle the error
    }

    private val mBroadcastReceiver1: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action: String? = intent.getAction()
            // When discovery finds a device
            if (action == BluetoothAdapter.ACTION_SCAN_MODE_CHANGED) {
                val state: Int =
                    intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_OFF -> Log.d(TAG, "onReceive: STATE OFF")
                    BluetoothAdapter.STATE_TURNING_OFF -> Log.d(
                        TAG,
                        "mBroadcastReceiver1: STATE TURNING OFF"
                    )
                    BluetoothAdapter.STATE_ON -> Log.d(TAG, "mBroadcastReceiver1: STATE ON")
                    BluetoothAdapter.STATE_TURNING_ON -> Log.d(
                        TAG,
                        "mBroadcastReceiver1: STATE TURNING ON"
                    )
                }
            }
        }
    }

    //Para ver los cambios de estado del bluetooth, si se enciende o expira discovery
    private val mBroadcastReceiver2: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action: String? = intent.getAction()
            if (action == BluetoothAdapter.ACTION_SCAN_MODE_CHANGED) {
                val mode: Int =
                    intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR)
                when (mode) {
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE -> Log.d(
                        TAG,
                        "mBroadcastReceiver2: Discoverability Enabled."
                    )
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE -> Log.d(
                        TAG,
                        "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections."
                    )
                    BluetoothAdapter.SCAN_MODE_NONE -> Log.d(
                        TAG,
                        "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections."
                    )
                    BluetoothAdapter.STATE_CONNECTING -> Log.d(
                        TAG,
                        "mBroadcastReceiver2: Connecting...."
                    )
                    BluetoothAdapter.STATE_CONNECTED -> Log.d(
                        TAG,
                        "mBroadcastReceiver2: Connected."
                    )
                }
            }
        }
    }

    //Para recibir la lista de dispositivos disponibles btnDiscover
    private val mBroadcastReceiver3: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent) {
            val action: String? = intent.getAction()
            Log.d(TAG, "onReceive: ACTION FOUND.")
            if (action == BluetoothDevice.ACTION_FOUND) {
                val device: BluetoothDevice? =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                mBTDevices.add(device!!)
                Log.d(TAG, "onReceive: " + device?.getName().toString() + ": " + device.address)
                mDeviceListAdapter =
                    DeviceListAdapter(context!!, R.layout.device_adapter_view, mBTDevices)
                lvNewDevices?.adapter = mDeviceListAdapter
            }
            mDeviceListAdapter!!.notifyDataSetChanged();
            lvNewDevices!!.invalidateViews();
            lvNewDevices!!.refreshDrawableState();

        }
    }

    //Broadcast Receiver that detects bond state changes (Pairing status changes)
    private val mBroadcastReceiver4: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent) {
            val action: String? = intent.getAction()
            if (action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                val mDevice: BluetoothDevice? =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                //3 cases:
                //case1: bonded already
                if (mDevice?.getBondState() === BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.")
                    //inside BroadcastReceiver4
                    mBTDevice = mDevice
                }
                //case2: creating a bone
                if (mDevice?.getBondState() === BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.")
                }
                //case3: breaking a bond
                if (mDevice?.getBondState() === BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.")
                }
            }
        }
    }

    fun enviarCaracter(caracter: String) {
        val bytes: ByteArray = caracter.toByteArray(Charset.defaultCharset())
        mBluetoothConnection?.write(bytes)
    }

    @SuppressLint("MissingPermission")
    fun buscarBT() {

        mBTDevices = ArrayList<BluetoothDevice>()

        checkBTPermissions()

        if (mBluetoothAdapter!!.isDiscovering()) {
            mBluetoothAdapter!!.cancelDiscovery()
            checkBTPermissions()
            mBluetoothAdapter?.startDiscovery()
            val discoverDevicesIntent = IntentFilter(BluetoothDevice.ACTION_FOUND)
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent)
        }
        if (!mBluetoothAdapter?.isDiscovering()!!) {
            checkBTPermissions()
            mBluetoothAdapter?.startDiscovery()
            val discoverDevicesIntent = IntentFilter(BluetoothDevice.ACTION_FOUND)
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent)
        }

    }

    //Método para iniciar conexión.
    fun startConnection() {
        startBTConnection(mBTDevice, MY_UUID_INSECURE)
    }

    fun startBTConnection(device: BluetoothDevice?, uuid: UUID?) {
        Log.d("ERROR", "Iniciando conexión ")
        mBluetoothConnection?.startClient(device, uuid)
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkBTPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            var permissionCheck: Int =
                this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION")
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION")
            var permiso2: Int = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            );
            if (permissionCheck != 0 && permiso2 != 0) {
                this.requestPermissions(
                    arrayOf<String>(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ), 1001
                ) //Any number
            }
        } else {
            Log.d("ERROR", "Permisos denegador")
        }
    }

    @SuppressLint("MissingPermission")
    fun enableDisableBT() {
        if (mBluetoothAdapter == null) {
            Log.e("ERROR", "No tiene bluetooth")
        }
        if (!mBluetoothAdapter!!.isEnabled) {
            val enableBTIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(enableBTIntent)
            val BTIntent = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            registerReceiver(mBroadcastReceiver1, BTIntent)
        }
        if (mBluetoothAdapter!!.isEnabled) {
            mBluetoothAdapter!!.disable()
            val BTIntent = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            registerReceiver(mBroadcastReceiver1, BTIntent)
        }
    }
}