package com.teammortys.mortysappkotlin

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.*

class BluetoothConnectionService(var mContext: Context) {
    private val mBluetoothAdapter: BluetoothAdapter
    private var mInsecureAcceptThread: AcceptThread? = null
    private var mConnectThread: ConnectThread? = null
    private var mmDevice: BluetoothDevice? = null
    private var deviceUUID: UUID? = null
    var mProgressDialog: ProgressDialog? = null
    private var mConnectedThread: ConnectedThread? = null

    //Servidor Bluetooth
    @SuppressLint("MissingPermission")
    private inner class AcceptThread : Thread() {
        // Socket servidor Lo9cal bluetooth
        private val mmServerSocket: BluetoothServerSocket?
        override fun run() {
            var socket: BluetoothSocket? = null
            try {
                socket = mmServerSocket!!.accept()

            } catch (e: IOException) {
                Log.e("ERROR", "Excepción coneción servidor " + e.message)
            }

            socket?.let { connected(it, mmDevice) }

        }

        fun cancel() {

            try {
                mmServerSocket!!.close()
            } catch (e: IOException) {
                Log.e("Cancrlar", "Cancelar" + e.message)
            }
        }

        init {
            var tmp: BluetoothServerSocket? = null

            // Crear una nueva conexión
            try {
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
                    appName,
                    MY_UUID_INSECURE
                )

            } catch (e: IOException) {
                Log.e("ERROR", "Execpcion " + e.message)
            }
            mmServerSocket = tmp
        }
    }

   // Cliente de bluetooth
    private inner class ConnectThread(device: BluetoothDevice?, uuid: UUID?) :
        Thread() {
        private var mmSocket: BluetoothSocket? = null
        @SuppressLint("MissingPermission")
        override fun run() {
            var tmp: BluetoothSocket? = null
            try {

                tmp = mmDevice!!.createRfcommSocketToServiceRecord(deviceUUID)
            } catch (e: IOException) {
                Log.e("ERROR", "Errpr de copnexion " + e.message)
            }
            mmSocket = tmp

            // Camcelar el discoveri, pone lenta la conexión
            mBluetoothAdapter.cancelDiscovery()

            // Conectarse con el socket de bluetooth
            try {

                mmSocket!!.connect()
                Log.d("ZCEPTAR", "YA se conecto")
            } catch (e: IOException) {
                try {
                    mmSocket!!.close()
                } catch (e1: IOException) {
                    Log.e("ERROR", "No se pudo cerrar la conexion" + e1.message
                    )
                }
                Log.d("ERROR", "NO SE PUEDE CONECTAR con el UUID " + MY_UUID_INSECURE)
                e.printStackTrace()
            }

            connected(mmSocket, mmDevice)
        }

        fun cancel() {
            try {
                mmSocket!!.close()
            } catch (e: IOException) {
                Log.e("ERROR", "FALLO LA CONCELACIÓN " + e.message)
            }
        }

        init {
            mmDevice = device
            deviceUUID = uuid
        }
    }


    // inicio de la transferencia de datos

    @Synchronized
    fun start() {
        Log.d("INICIO", "INICIO DE CONEXIÓN ")


        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = AcceptThread()
            mInsecureAcceptThread!!.start()
        }
    }

    //Intenta conectase con el dispsotivo bluetooth
    fun startClient(device: BluetoothDevice?, uuid: UUID?) {

      //Inicia la conexión
        mProgressDialog = ProgressDialog.show(
            mContext, "Conectado al bluetooth", "Espere...", true
        )
        mConnectThread = ConnectThread(device, uuid)
        mConnectThread!!.start()
    }

    //Clase que se encarga de mantener la conexión, enviar y recibir datos
    private inner class ConnectedThread(socket: BluetoothSocket?) : Thread() {
        private val mmSocket: BluetoothSocket?
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?
        override fun run() {
            val buffer = ByteArray(1024) // buffer store para transmitor
            var bytes: Int

            while (true) {
                // Hacxer la lectura desde le inpurStream
                try {
                    bytes = mmInStream!!.read(buffer)
                    val incomingMessage = String(buffer, 0, bytes)
                    Log.d(
                        "Recibido",
                        "InputStream: $incomingMessage"
                    )

                    var incomingMessageIntent: Intent = Intent("incomingMessage")
                    incomingMessageIntent.putExtra("Message", incomingMessage)
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(incomingMessageIntent)
                } catch (e: IOException) {
                    Log.e(TAG, "Error al leer el mensaje. " + e.message)
                    break
                }
            }
        }

        //Metodo para enviar un dato
        fun write(bytes: ByteArray?) {
            val text = String(bytes!!, Charset.defaultCharset())
            Log.e(
                "Escribiendo",
                "Escribiendo: Salida: $text"
            )
            try {
                mmOutStream!!.write(bytes)
            } catch (e: IOException) {
                Log.e("Escribiendo", "ERROR al escribir e mensaje. " + e.message)
            }
        }

        fun cancel() {
            try {
                mmSocket!!.close()
            } catch (e: IOException) {
            }
        }

        init {

            mmSocket = socket
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            try {
                mProgressDialog!!.dismiss()
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
            try {
                tmpIn = mmSocket!!.inputStream
                tmpOut = mmSocket.outputStream
            } catch (e: IOException) {
                e.printStackTrace()
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut
        }
    }

    private fun connected(mmSocket: BluetoothSocket?, mmDevice: BluetoothDevice?) {

        mConnectedThread = ConnectedThread(mmSocket)
        mConnectedThread!!.start()
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread.write
     */
    fun write(out: ByteArray?) {
        // Crear un objeto twemporal
        var r: ConnectedThread

        //Escribiendo
        mConnectedThread!!.write(out)
    }

    companion object {
        private const val TAG = "BluetoothConnectionServ"
        private const val appName = "MYAPP"
        private val MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")
    }

    init {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        start()
    }
}
