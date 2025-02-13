package com.sri.nfcdemo

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.NdefFormatable
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sri.nfcdemo.ui.theme.NFCDemoTheme

class MainActivity : ComponentActivity() {
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var tagId by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not supported in this phone", Toast.LENGTH_LONG).show()
        }
        nfcAdapter?.let {
            if (it.isEnabled) {
                Toast.makeText(this, "NFC is activated", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(
                    this,
                    "NFC is not active",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
        pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE,
        )
        setContent {
            NFCDemoTheme {
                ScannerFrame(tagId = tagId)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action
        ) {
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            tag?.let {
                val techList = it.techList.joinToString(",")
                Log.d("NFC_DUBUG", "Show techList = $techList")
                tagId = it.id.joinToString(":") { byte -> String.format("%02x", byte) }
                isTagFormattable(tag = it, "test")
            } ?: Log.d("NFC ERROR", "TAG not detected ")
            Toast.makeText(this, "the tag is $tagId", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Activer le mode NFC pour recevoir les intents
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        // Désactiver la réception des intents NFC quand l'application est en arrière-plan
        nfcAdapter?.disableForegroundDispatch(this)
    }
}

@Composable
fun ScannerFrame(tagId: String) {
    val infiniteTransaction = rememberInfiniteTransition()
    val offsetY by infiniteTransaction.animateFloat(
        initialValue = 0f,
        targetValue = 800f, // Hauteur du cadre de scan
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bare de scan",
    )
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        if (tagId.isNotEmpty()) Text(text = tagId, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Red, modifier = Modifier.padding(all = 8.dp))
        Box(modifier = Modifier.size(300.dp).border(width = 2.dp, color = Color.Black)) {
            Canvas(
                modifier = Modifier.fillMaxSize(),
            ) {
                drawLine(
                    color = Color.Red,
                    strokeWidth = 4f,
                    start = Offset(0f, offsetY),
                    end = Offset(size.width, offsetY),
                )
            }
        }
    }
}

private fun isTagFormattable(tag: Tag?, message: String): Boolean {
    val ndefFormattable = NdefFormatable.get(tag)

    try {
        if (ndefFormattable != null) {
            ndefFormattable.connect()
            val ndefMessage = createNdefMessage(message = message)
            ndefFormattable.format(ndefMessage)
            ndefFormattable.close()
            Log.d("NDEF_DEBUG", "Tag formaté avec succée")
            return true
        } else {
            Log.d("NDEF_DEBUG", "Tag n'est pas formattable en NDEF")

            return false
        }
    } catch (e: Exception) {
        Log.e("NDEF_ERROR", "${e.message}")
    }

    return false
}

fun createNdefMessage(message: String): NdefMessage {
    val record = NdefRecord.createTextRecord("en", message)
    return NdefMessage(arrayOf(record))
}

private fun writeToMifareClassicTag(tag: Tag?, data: ByteArray) {
    val mifareClassic = MifareClassic.get(tag)
    try {
        mifareClassic.connect()
        // Par exemple, écrire dans le premier secteur
        val sectorIndex = 0
        val blockIndex = 0
        if (mifareClassic.authenticateSectorWithKeyB(sectorIndex, MifareClassic.KEY_DEFAULT)) {
            mifareClassic.writeBlock(mifareClassic.sectorToBlock(sectorIndex) + blockIndex, data)
            Log.d("NFC_DEBUG", "Données écrites avec succès sur le tag Mifare Classic.")
        } else {
            Log.e("NFC_DEBUG", "Échec de l'authentification du secteur.")
        }
    } catch (e: Exception) {
        Log.e("NFC_DEBUG", "Erreur lors de l'écriture sur le tag Mifare Classic: ${e.message}")
    } finally {
        mifareClassic.close()
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NFCDemoTheme {
        ScannerFrame("test")
    }
}
