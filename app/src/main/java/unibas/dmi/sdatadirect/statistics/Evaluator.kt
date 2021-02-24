package unibas.dmi.sdatadirect.statistics

import android.content.Context
import java.io.FileOutputStream
import java.io.FileWriter
import android.util.Log
import java.io.File

class Evaluator(){
    companion object {
        val TAG = "Evaluator"
        var bytesSentNaive: Long = 0L
        var packagesSentNaive: Long = 0L
        var bytesReceivedNaive: Long = 0L
        var packagesReceivedNaive: Long = 0L
        var syncTimeNaive: Long = 0L
        var bytesSentHistoryAware: Long = 0L
        var packagesSentHistoryAware: Long = 0L
        var bytesReceivedHistoryAware: Long = 0L
        var packagesReceivedHistoryAware: Long = 0L
        var syncTimeHistoryAware: Long = 0L
        var context: Context? = null

        fun evaluate() {
            val w = File(context!!.getExternalFilesDir(null), "testRes.txt")
            w.createNewFile()
            w.appendText("-------------------------Naive Method Performance-------------------------\n")
            w.appendText("Time to sync: $syncTimeNaive\n")
            w.appendText("Bytes sent: $bytesSentNaive\n")
            w.appendText("Bytes received: $bytesReceivedNaive\n")
            w.appendText("Packages sent: $packagesSentNaive\n")
            w.appendText("Packages received: $packagesReceivedNaive\n")
            w.appendText("-------------------------History Aware Method Performance-------------------------\n")
            w.appendText("Time to sync: $syncTimeHistoryAware\n")
            w.appendText("Bytes sent: $bytesSentHistoryAware\n")
            w.appendText("Bytes received: $bytesReceivedHistoryAware\n")
            w.appendText("Packages sent: $packagesSentHistoryAware\n")
            w.appendText("Packages received: $packagesReceivedHistoryAware\n")
            w.appendText("-------------------------Results-------------------------\n")
            if ((bytesSentNaive + bytesReceivedNaive) > (bytesReceivedHistoryAware + bytesSentHistoryAware)) {
                w.appendText(
                    "The history aware method performed better in terms of bytes exchanged,\n" +
                            "the devices exchanged ${(bytesSentNaive + bytesReceivedNaive) - (bytesReceivedHistoryAware + bytesSentHistoryAware)} bytes fewer.\n"
                )
            } else {
                w.appendText(
                    "The Naive method performed better in terms of bytes exchanged,\n" +
                            "the devices exchanged ${(bytesSentHistoryAware + bytesReceivedHistoryAware) - (bytesReceivedNaive + bytesSentNaive)} bytes fewer.\n"
                )
            }
            if ((packagesReceivedNaive + packagesSentNaive) > (packagesReceivedHistoryAware + packagesSentHistoryAware)) {
                w.appendText(
                    "The history aware method performed better in terms of packages exchanged,\n" +
                            "the devices exchanged ${(packagesSentNaive + packagesReceivedNaive) - (packagesReceivedHistoryAware + packagesSentHistoryAware)} packages fewer.\n"
                )
            } else {
                w.appendText(
                    "The naive method performed better in terms of packages exchanged,\n" +
                            "the devices exchanged ${(packagesSentHistoryAware + packagesReceivedHistoryAware) - (packagesReceivedNaive + packagesSentNaive)} packages fewer.\n"
                )
            }
            if (syncTimeNaive > syncTimeHistoryAware) {
                w.appendText(
                    "The history aware method performed better in terms of synchronization time,\n" +
                            "the devices synchronized ${syncTimeNaive - syncTimeHistoryAware}ms faster.\n"
                )
            } else {
                w.appendText(
                    "The Naive method performed better in terms of synchronization time,\n" +
                            "the devices synchronized ${syncTimeHistoryAware - syncTimeNaive}ms faster.\n"
                )
            }
            Log.d(TAG,w.absolutePath)
        }
    }
}