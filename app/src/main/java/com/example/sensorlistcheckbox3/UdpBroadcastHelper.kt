package com.example.sensorlistcheckbox3

import android.os.AsyncTask
import android.util.Log
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UdpBroadcastHelper {

    // TODO translated from Java: convert to proper Kotlin
    fun sendUdpBroadcast(name: String, values: FloatArray, port: Int) {
        val message = "$name\n${values.joinToString(" ")}"
        Log.d("MINE", message)
        sendUdpBroadcastAsync(message, port)
    }

    fun sendUdpBroadcast(message: String, port: Int) {
        Log.d("MINE", message)
        sendUdpBroadcastAsync(message, port)
    }


    private fun sendUdpBroadcastAsync(message: String, port: Int) {
        val broadCaster = UdpBroadCaster()
        // https://stackoverflow.com/questions/10135910/is-there-a-constructor-associated-with-nested-classes
        val mp = broadCaster.MessagePort(message, port)
        broadCaster.execute(mp)
    }

    // Networking must be done in a background thread/task
    // TODO modern alternative to AsyncTask
    internal class UdpBroadCaster : AsyncTask<UdpBroadCaster.MessagePort, Void, Void>() {
        internal inner class MessagePort(val message: String, val port: Int)

        override fun doInBackground(vararg messagePorts: MessagePort): Void? {
            try {
                sendUdpBroadcast(messagePorts[0].message, messagePorts[0].port)
            } catch (e: IOException) {
                Log.e("MINE", e.message, e)
            }
            return null
        }

        @Throws(IOException::class)
        fun sendUdpBroadcast(messageStr: String, port: Int) {
            val broadcastIP = "255.255.255.255"
            val inetAddress: InetAddress = InetAddress.getByName(broadcastIP)
            val socket = DatagramSocket()
            // todo try with resource? requires higher api level 19
            // https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html
            socket.broadcast = true
            val sendData = messageStr.toByteArray()
            val sendPacket = DatagramPacket(sendData, sendData.size, inetAddress, port)
            socket.send(sendPacket)
            Log.d("MINE", "Broadcast sent: $messageStr")
            socket.close()
        }
    }
}