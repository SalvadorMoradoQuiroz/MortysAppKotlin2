package com.teammortys.mortysappkotlin

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.*
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.*
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.switchmaterial.SwitchMaterial
import com.teammortys.mortysappkotlin.ObjectDetectorHelper.*
import org.tensorflow.lite.task.vision.detector.Detection
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.Charset
import java.util.*


class MainActivity : AppCompatActivity(), View.OnClickListener, DetectorListener,
    AdapterView.OnItemClickListener {

    companion object {
        private const val TAG = "MainActivity"
        private val MY_UUID_INSECURE: UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")
    }

    private var dialog: Dialog? = null

    //BT
    var btnCerrar: Button? = null
    var btnVerBT: Button? = null
    var btnBuscar: Button? = null
    var mBluetoothAdapter: BluetoothAdapter? = null
    var btnOnOF: Button? = null
    var mBluetoothConnection: BluetoothConnectionService? = null
    var btnConectar: Button? = null
    var mBTDevice: BluetoothDevice? = null
    var mBTDevices: java.util.ArrayList<BluetoothDevice> = ArrayList<BluetoothDevice>()
    var mDeviceListAdapter: DeviceListAdapter? = null
    var lvNewDevices: ListView? = null
    var dialogBT: Dialog? = null
    var dialogListBT: Dialog? = null

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

    private var flash_on_off: Boolean = false
    private var flagVideo = false

    private lateinit var objectDetectorHelper: ObjectDetectorHelper

    private lateinit var tracking_overlay: OverlayView
    private var imageMonitor: ImageView? = null
    private var button_Video: Button? = null
    private var switch_ObjDetect: SwitchMaterial? = null
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
        //mBluetoothAdapter.cancelDiscovery();
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

        imageButton_ClawClose!!.setOnClickListener(this);
        imageButton_Up!!.setOnClickListener(this);
        imageButton_Down!!.setOnClickListener(this);
        imageButton_ClawOpen!!.setOnClickListener(this);
        imageButton_ZipperUp!!.setOnClickListener(this);
        imageButton_Right!!.setOnClickListener(this);
        imageButton_Left!!.setOnClickListener(this);
        imageButton_ZipperDown!!.setOnClickListener(this);
        button_Video!!.setOnClickListener(this);

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


        /*textViewObjectDetected = findViewById(R.id.textViewObjectDetected)

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, IntentFilter("incomingMessage"))
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

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
        val id = item.getItemId()

        if (id == R.id.sw_bluewifi) {
            //Toast.makeText(this, "Item One Clicked", Toast.LENGTH_LONG).show()
            return true
        }
        if (id == R.id.conf_bt) {
            //Toast.makeText(this, "Item Two Clicked", Toast.LENGTH_LONG).show()
            /*val builder = AlertDialog.Builder(this,R.style.Theme_MortysAppKotlin)
                .create()
            val view = layoutInflater.inflate(R.layout.layout_config_alert,null)
            val  btnConectar = view.findViewById<Button>(R.id.buttonConnect)
            builder.setView(view)
            btnConectar.setOnClickListener {
                builder.dismiss()
            }
            builder.setCanceledOnTouchOutside(false)
            builder.show()*/
            this@MainActivity.dialogBT = Dialog(this@MainActivity)
            this@MainActivity.dialogBT!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
            this@MainActivity.dialogBT!!.setCancelable(false)
            this@MainActivity.dialogBT!!.setContentView(R.layout.layout_config_bt)

            this@MainActivity.btnVerBT = this@MainActivity.dialogBT!!.findViewById(R.id.btn_visible)
            this@MainActivity.btnOnOF = this@MainActivity.dialogBT!!.findViewById(R.id.bt_onoff)
            this@MainActivity.btnConectar =
                this@MainActivity.dialogBT!!.findViewById(R.id.btn_conectar)
            this@MainActivity.btnBuscar = this@MainActivity.dialogBT!!.findViewById(R.id.btn_buscar)
            this@MainActivity.btnCerrar =
                this@MainActivity.dialogBT!!.findViewById(R.id.btn_cgarrad)
            this@MainActivity.btnCerrar!!.setOnClickListener(this)

            this@MainActivity.btnVerBT!!.setOnClickListener(this)
            this@MainActivity.btnOnOF!!.setOnClickListener(this)
            this@MainActivity.btnConectar!!.setOnClickListener(this)
            this@MainActivity.btnBuscar!!.setOnClickListener(this)


            this@MainActivity.dialogBT!!.show()
            return true
        }

        if (id == R.id.conf_conexion) {
            //Toast.makeText(this, "Item Two Clicked", Toast.LENGTH_LONG).show()
            /*val builder = AlertDialog.Builder(this,R.style.Theme_MortysAppKotlin)
                .create()
            val view = layoutInflater.inflate(R.layout.layout_config_alert,null)
            val  btnConectar = view.findViewById<Button>(R.id.buttonConnect)
            builder.setView(view)
            btnConectar.setOnClickListener {
                builder.dismiss()
            }
            builder.setCanceledOnTouchOutside(false)
            builder.show()*/
            this@MainActivity.dialog = Dialog(this@MainActivity)
            this@MainActivity.dialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
            this@MainActivity.dialog!!.setCancelable(false)
            this@MainActivity.dialog!!.setContentView(R.layout.layout_config_alert)

            /*this@MainActivity.buttonConnect =
                this@MainActivity.dialog!!.findViewById(R.id.buttonConnect)
            this@MainActivity.editTextIP = this@MainActivity.dialog!!.findViewById(R.id.editTextIP)
            this@MainActivity.buttonFlash =
                this@MainActivity.dialog!!.findViewById(R.id.buttonDisconnect)
            this@MainActivity.editTextIP!!.setText("192.168.43.180")
            this@MainActivity.buttonFlash!!.setOnClickListener(this)
            this@MainActivity.buttonDisconnect =
                this@MainActivity.dialog!!.findViewById(R.id.buttonDisconnect)
            this@MainActivity.btnCerrar = this@MainActivity.dialog!!.findViewById(R.id.btn_cgarraf)
            this@MainActivity.btnCerrar!!.setOnClickListener(this)
            this@MainActivity.buttonConnect!!.setOnClickListener(this@MainActivity)
            this@MainActivity.buttonFlash!!.setOnClickListener(this@MainActivity)
            this@MainActivity.buttonDisconnect!!.setOnClickListener(this@MainActivity)
            this@MainActivity.dialog!!.show()*/
            return true
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
        flash_on_off = flash_on_off xor true
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
                    imageMonitor!!.setImageBitmap(bitmap)
                }

                //objectDetectorHelper.detect(bitmap, 90)

            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
        runOnUiThread { imageMonitor!!.setImageResource(R.drawable.play_video) }
    }

    //Listeners Object detect-----------------------------------------------------------------------
    override fun onError(error: String) {
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

    //Para recibir un mensaje y mostrarlo en una caja de texo
    var mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            var text: String? = intent.getStringExtra("Message")
            Log.d("Mensaje", "$text")
            /*messages?.append("$text \n")
            MessageIncoming?.setText(messages)*/
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

    @SuppressLint("MissingPermission")
    override fun onItemClick(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
        dialogListBT?.dismiss()
        mBluetoothAdapter?.cancelDiscovery()

        //val deviceName: String = mBTDevices.get(i).getName()
        val deviceAddress: String = mBTDevices[i].address

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mBTDevices.get(i).createBond()
            mBTDevice = mBTDevices[i]
            mBluetoothConnection = BluetoothConnectionService(this@MainActivity)
        }
        this@MainActivity.dialogBT = Dialog(this@MainActivity)
        this@MainActivity.dialogBT!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        this@MainActivity.dialogBT!!.setCancelable(false)
        this@MainActivity.dialogBT!!.setContentView(R.layout.layout_config_bt)

        this@MainActivity.btnVerBT = this@MainActivity.dialogBT!!.findViewById(R.id.btn_visible)
        this@MainActivity.btnOnOF = this@MainActivity.dialogBT!!.findViewById(R.id.bt_onoff)
        this@MainActivity.btnConectar = this@MainActivity.dialogBT!!.findViewById(R.id.btn_conectar)
        this@MainActivity.btnBuscar = this@MainActivity.dialogBT!!.findViewById(R.id.btn_buscar)

        this@MainActivity.btnVerBT!!.setOnClickListener(this)
        this@MainActivity.btnOnOF!!.setOnClickListener(this)
        this@MainActivity.btnConectar!!.setOnClickListener(this)
        this@MainActivity.btnBuscar!!.setOnClickListener(this)


        this@MainActivity.dialogBT!!.show()
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
            Log.d("ERROR", "No tiene bluetooth")
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