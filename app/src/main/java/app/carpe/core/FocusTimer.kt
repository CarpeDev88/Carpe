package app.carpe.core
import android.os.CountDownTimer
class FocusTimer(private val minutes:Int,private val tick:(Long)->Unit,private val done:()->Unit){
 private var timer:CountDownTimer?=null
 fun start(){timer?.cancel();timer=object:CountDownTimer(minutes*60000L,1000L){override fun onTick(ms:Long)=tick(ms);override fun onFinish()=done()}.start()}
 fun cancel(){timer?.cancel()}
}