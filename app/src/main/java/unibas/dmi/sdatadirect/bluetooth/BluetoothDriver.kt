package unibas.dmi.sdatadirect.bluetooth

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import unibas.dmi.sdatadirect.MainActivity
import unibas.dmi.sdatadirect.crypto.CryptoHandler
import unibas.dmi.sdatadirect.database.Peer
import unibas.dmi.sdatadirect.peer.PeerViewModel
import unibas.dmi.sdatadirect.ui.BluetoothDeviceListAdapter
import unibas.dmi.sdatadirect.utils.QRCode
import java.io.ByteArrayInputStream
import java.io.IOException
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList
import kotlin.system.exitProcess

/**
 * BluetoothDriver contains all parameters and functions that helps to scan, pair and connect to
 * other bluetooth devices.
 *
 * Remark: If you like to work with Bluetooth Low Energy technology, consider the class BLEDriver
 *
 * Reference: https://developer.android.com/guide/topics/connectivity/bluetooth
 */
class BluetoothDriver(
    val activity: MainActivity,
    val handler: Handler,
    val qrCode: QRCode,
    val cryptoHandler: CryptoHandler,
    val peerViewModel: PeerViewModel) {

    private val TAG: String = "BluetoothDriver"

    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private val NAME: String = "sDaTaDirect"
    private val MY_UUID: UUID = UUID.fromString("6696ce56-49ac-459e-b4cf-502247bd8f21")

    var acceptThread: AcceptThread? = null  // Server
    var connectThread: ConnectThread? = null    // Client

    val receiver: BluetoothBroadcastReceiver = BluetoothBroadcastReceiver(this, activity)

    val devices: ArrayList<BluetoothDevice?> = ArrayList()
    lateinit var bluetoothDeviceListAdapter: BluetoothDeviceListAdapter

    /**
     * Checks whether the device contains a Bluetooth adapter or not. Quite all devices have a
     * Bluetooth adapter and therefore no checking needed.
     */
    fun checkForBluetoothAdapter(): Boolean {
        if (bluetoothAdapter == null) {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle("No Bluetooth Adapter")
            builder.setMessage("No bluetooth adapter could be found on this device. This app" +
                    "cannot be further deployed.")
            builder.setNeutralButton("Close app") {_,_ ->
                activity.finish()
                exitProcess(0)
            }

            val dialog: AlertDialog = builder.create()
            dialog.show()

            return false
        } else {
            return true
        }
    }

    /**
     * Removes bonded Bluetooth devices after ending the session.
     */
    fun removePairs(device: BluetoothDevice?) {
        try {
            device!!::class.java.getMethod("removeBond").invoke(device)
        } catch (e: Exception) {
            Log.d(TAG, "Removing bond has been failed ${e.message}")
        }

    }

    /**
     * Makes the device discoverable for other devices.
     */
    fun discoverable(enabled: Boolean) {
        if (enabled) {
            Log.d(TAG, "Making device discoverable")
            val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 100)
            activity.startActivity(discoverableIntent)
        } else {
            Log.d(TAG, "Making device not discoverable anymore")
            val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 1)
            activity.startActivity(discoverableIntent)
        }
    }

    /**
     * Starts discovery and looks for available devices in the surrounding.
     */
    fun startDiscovery() {
        Log.d(TAG, "Looking for unpaired devices.");
        bluetoothAdapter?.startDiscovery()
    }

    /**
     * Start AcceptThread (server) to begin a session in listening for incoming connections.
     */
    fun startServer() {
        Log.d(TAG, "Starting server thread and waiting for incoming connections ...")
        acceptThread = AcceptThread()
        acceptThread?.start()
    }

    /**
     * Start ConnectThread (client) to attempt a connection with a server device.
     */
    fun connect(device: BluetoothDevice?) {
        Log.d(TAG, "Connecting Bluetooth ...")

        val message: Message = Message.obtain()
        message.what = activity.STATE_CONNECTING
        handler.sendMessage(message)

        connectThread = ConnectThread(device)
        connectThread?.start()
    }

    fun stop() {
        connectThread?.cancel()
        acceptThread?.cancel()
        Log.d(TAG, "Bluetooth Driver stopped")
    }

    /**
     * A thread for the server component that accepts incoming connections. The component start
     * listening for connection requests by calling accept(). The connections is accepted if the
     * connection request contains a UUID that matches the one registered with this listening
     * server socket. After accepting the request, it returns a connected BluetoothSocket.
     *
     * Remark:
     * - accept() is a blocking call. Any work involving BluetoothServerSocket should be done in a separate thread.
     * - close() releases server socket and all its resources, but does not close the connected BluetoothSocket.
     */
    inner class AcceptThread: Thread() {
        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(NAME, MY_UUID)
        }
        var socket: BluetoothSocket? = null
        private val mmBuffer: ByteArray = ByteArray(1024)

        override fun run() {
            // Keep listening until exception occurs or a socket is returned.
            var shouldLoop = true

            while (shouldLoop) {

                try {
                    socket = mmServerSocket?.accept()
                    Log.d(TAG, socket?.remoteDevice?.name)
                    val message: Message = Message.obtain()
                    message.what = activity.STATE_CONNECTING
                    handler.sendMessage(message)
                } catch (e: IOException) {
                    Log.e(TAG, "Socket's accept() method failed", e)
                    val message: Message = Message.obtain()
                    message.what = activity.STATE_CONNECTION_FAILED
                    handler.sendMessage(message)
                    shouldLoop = false
                }

                socket?.also {
                    val message: Message = Message.obtain()
                    message.what = activity.STATE_CONNECTED
                    handler.sendMessage(message)
                    activity.wifiP2pDriver.wantsToBeClient = false


                    while(true){

                        var peer = peerViewModel.getPeerByBluetoothAddress(it.remoteDevice.address)

                        if (peer == null) {
                           lateinit var newPeer: Peer
                            if (peerViewModel.getByPublicKey(qrCode.scannedContent?.split("#")?.get(2)!!) == null) {
                                newPeer = Peer(
                                    name = it.remoteDevice.name,
                                    bluetooth_mac_address = it.remoteDevice.address,
                                    wifi_mac_address = qrCode.scannedContent?.split("#")?.get(0),
                                    shared_key = cryptoHandler.getSecretKeyEncoded(cryptoHandler.secretAESKey!!),
                                    public_key = cryptoHandler.getPublicKeyEncoded(cryptoHandler.publicRSAKey!!),
                                    private_key = cryptoHandler.getPrivateKeyEncoded(cryptoHandler.privateRSAKey!!),
                                    foreign_public_key = qrCode.scannedContent?.split("#")?.get(2)
                                )
                            } else {
                                newPeer = Peer(
                                    name = it.remoteDevice.name,
                                    bluetooth_mac_address = it.remoteDevice.address,
                                    wifi_mac_address = qrCode.scannedContent?.split("#")?.get(0),
                                    shared_key = cryptoHandler.getSecretKeyEncoded(cryptoHandler.secretAESKey!!),
                                    public_key = cryptoHandler.getPublicKeyEncoded(cryptoHandler.publicRSAKey!!),
                                    private_key = cryptoHandler.getPrivateKeyEncoded(cryptoHandler.privateRSAKey!!)
                                    )
                            }

                            peerViewModel.insert(newPeer)
                            val message: Message = Message.obtain()
                            message.what = activity.PEER_SAVED
                            handler.sendMessage(message)

                            sleep(3000)
                        } else {

                            activity.wifiP2pDriver.clientAddress = peer?.wifi_mac_address!!
                            activity.wifiP2pDriver.targetDeviceAddress = peer?.wifi_mac_address!!

                            val sharedKey = cryptoHandler.getSecretKeyDecoded(peer.shared_key!!)
                            val signature =
                                cryptoHandler.createSignature(sharedKey.encoded, peer.private_key)

                            val objectMapper = ObjectMapper()

                            val signatureToString = Base64.getEncoder().encodeToString(signature)

                            val json = "{\"signature\" : \"$signatureToString\"}"

                            val node = objectMapper.readTree(json)
                            val encodedNode = objectMapper.writeValueAsBytes(node)

                            write(encodedNode)

                            val remotePeer =
                                peerViewModel.getPeerByBluetoothAddress(it.remoteDevice.address)
                            val sharedKey2 =
                                cryptoHandler.getSecretKeyDecoded(remotePeer?.shared_key!!)

                            try {
                                socket!!.inputStream.read(mmBuffer)
                            } catch (e: IOException) {
                                Log.e(TAG, e.message)
                            }

                            val decodedNode = objectMapper.readTree(ByteArrayInputStream(mmBuffer))

                            val receivedSignature: ByteArray =
                                Base64.getDecoder().decode(decodedNode.get("signature").asText())

                            val verification = cryptoHandler.verifySignature(
                                receivedSignature,
                                sharedKey2.encoded,
                                remotePeer.foreign_public_key!!
                            )

                            // If verification was successful, phase 3 (WiFi-Direct) can be started
                            if (verification) {
                                val message = Message.obtain()
                                message.what = activity.VERIFICATION_SUCCESSFUL
                                handler.sendMessage(message)
                            } else {
                                val message = Message.obtain()
                                message.what = activity.VERIFICATION_FAILED
                                handler.sendMessage(message)
                            }

                            mmServerSocket?.close()
                            shouldLoop = false
                            break
                        }
                    }
                }
            }
        }


        private fun write(bytes: ByteArray) {
            try {
                socket?.outputStream?.write(bytes)
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when sending data", e)

                // Send a failure message back to the activity.
                val writeErrorMsg = handler.obtainMessage(activity.MESSAGE_TOAST)
                val bundle = Bundle().apply {
                    putString("toast", "Couldn't send data to the other device")
                }
                writeErrorMsg.data = bundle
                handler.sendMessage(writeErrorMsg)
                //return
            }
        }

        // Closes the connect socket and causes the thread to finish.
        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }

    /**
     * A thread for the client component, that issues a connection to a desired bluetooth device.
     * To establish a connection a BluetoothSocket from the device must be acquired. The client calls
     * the connect() method. The system will perform a SDP lookup to find the remote device with the
     * matching UUID. After the device accepted the connection, it shared the RFCOMM channel.
     *
     * Remark: connect() is a blocking call. Time out after about 12 seconds.
     */
    inner class ConnectThread(val device: BluetoothDevice?) : Thread() {

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device?.createRfcommSocketToServiceRecord(MY_UUID)
        }
        private val mmBuffer: ByteArray = ByteArray(1024)


        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter?.cancelDiscovery()
            mmSocket?.use { socket ->
                socket.connect()
                val message: Message = Message.obtain()
                message.what = activity.STATE_CONNECTED
                handler.sendMessage(message)
                Log.d(TAG, "run: ConnectThread connected.")

                val peer = peerViewModel.getPeerByBluetoothAddress(device?.address!!)

                activity.wifiP2pDriver.wantsToBeClient = true
                activity.wifiP2pDriver.targetDeviceAddress = peer?.wifi_mac_address!!
                activity.wifiP2pDriver.clientAddress = peer?.wifi_mac_address!!



                val sharedKey = cryptoHandler.getSecretKeyDecoded(peer.shared_key!!)
                Log.d(TAG, "Client sharedkey1: ${peer.shared_key}")
                val signature = cryptoHandler.createSignature(sharedKey.encoded, peer.private_key)

                val objectMapper = ObjectMapper()

                val signatureToString = Base64.getEncoder().encodeToString(signature)

                val json =  "{\"signature\" : \"$signatureToString\"}"

                val node = objectMapper.readTree(json)
                val encodedNode = objectMapper.writeValueAsBytes(node)

                write(encodedNode)

                try {
                    socket.inputStream.read(mmBuffer)
                } catch (e: IOException) {
                    Log.e(TAG, e.message)
                }

                val decodedNode = objectMapper.readTree(ByteArrayInputStream(mmBuffer))

                val receivedSignature: ByteArray = Base64.getDecoder().decode(decodedNode.get("signature").asText())

                val verification = cryptoHandler.verifySignature(
                    receivedSignature,
                    sharedKey.encoded,
                    peer.foreign_public_key!!
                )

                if (verification) {
                    val message = Message.obtain()
                    message.what = activity.VERIFICATION_SUCCESSFUL
                    handler.sendMessage(message)
                } else {
                    val message = Message.obtain()
                    message.what = activity.VERIFICATION_FAILED
                    handler.sendMessage(message)
                }
                socket.close()
            }

        }

        private fun write(bytes: ByteArray) {
            try {
                mmSocket?.outputStream?.write(bytes)
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when sending data", e)

                // Send a failure message back to the activity.
                val writeErrorMsg = handler.obtainMessage(activity.MESSAGE_TOAST)
                val bundle = Bundle().apply {
                    putString("toast", "Couldn't send data to the other device")
                }
                writeErrorMsg.data = bundle
                handler.sendMessage(writeErrorMsg)
                //return
            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }
    }
}
