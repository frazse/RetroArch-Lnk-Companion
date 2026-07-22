package com.example.retroachivementscompanion

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import java.net.DatagramPacket
import java.net.DatagramSocket

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var running = true
    private var socket: DatagramSocket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Passive HUD Mode: Prevent focus and interaction from controllers/keys
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        
        webView = WebView(this)
        setContentView(webView)
        webView.settings.javaScriptEnabled = true
        webView.setBackgroundColor(Color.BLACK)
        webView.loadDataWithBaseURL("https://retroarch.dual", DASHBOARD_HTML, "text/html", "UTF-8", null)
        startUdpListener()
    }

    private fun startUdpListener() {
        Thread {
            try {
                Log.d("RetroArchLnk", "Starting UDP listener on port 55432")
                val s = DatagramSocket(55432)
                socket = s
                val buffer = ByteArray(65535)
                while (running) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    s.receive(packet)
                    val json = String(packet.data, 0, packet.length)
                    Log.d("RetroArchLnk", "Received JSON: $json")
                    runOnUiThread {
                        webView.evaluateJavascript("update($json);", null)
                    }
                }
            } catch (e: Exception) {
                if (running) {
                    Log.e("RetroArchLnk", "Socket error", e)
                }
            } finally {
                socket?.close()
                socket = null
            }
        }.start()
    }

    // Consume all key events (buttons) to prevent controller interaction
    override fun dispatchKeyEvent(event: KeyEvent): Boolean = true

    // Consume all generic motion events (analog sticks)
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean = true

    override fun onDestroy() {
        running = false
        socket?.close()
        super.onDestroy()
    }

    companion object {
        private const val DASHBOARD_HTML = """<html><head><style>
        body { background-color: #0F111A; color: #E0E0E0; font-family: sans-serif; padding: 0; margin: 0; overflow: hidden; }
        .dashboard { position: fixed; top: 0; left: 0; right: 0; height: 120px; background: #1A1C2E; display: flex; flex-direction: column; padding: 8px 0 0 0; border-bottom: 4px solid #00BFA5; z-index: 100; box-sizing: border-box; }
        .game-title { font-size: 19px; font-weight: 800; color: #00BFA5; margin: 0 0 2px 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; width: 100%; text-align: center; }
        .telemetry-grid { display: flex; flex-direction: column; flex: 1; justify-content: center; width: 100%; }
        .telemetry-row { display: flex; justify-content: space-evenly; width: 100%; }
        .column { display: flex; flex-direction: column; align-items: center; width: 25%; }
        .val-text { font-size: 18px; font-weight: bold; color: #FFF; line-height: 1.0; margin-bottom: 2px; }
        .label { font-size: 10px; color: #888; font-weight: 900; text-transform: uppercase; letter-spacing: 0.5px; width: 25%; text-align: center; margin-top: 2px; }
        .anchored-row { position: relative; width: 100%; height: 20px; display: flex; align-items: center; }
        .game-progress { position: absolute; left: 10px; font-size: 12px; color: #888; font-weight: 900; }
        .clock-container { position: absolute; right: 10px; }
        .clock { font-size: 12px; font-weight: 900; color: #888; }
        .progress-bar-bg { width: 100%; height: 8px; background: #2A2E45; overflow: hidden; margin-top: auto; }
        .progress-bar-fill { height: 100%; background: #4CAF50; width: 0%; transition: width 0.5s; }
        .content { margin-top: 120px; height: calc(100vh - 120px); overflow-y: auto; padding: 18px; box-sizing: border-box; }
        .achievement { display: flex; align-items: center; margin-bottom: 12px; padding: 12px; background: #1E2132; border-radius: 10px; border: 1px solid #2A2E45; position: relative; overflow: hidden; }
        .achievement.unlocked { border-left: 4px solid #4CAF50; background: #242938; }
        .achievement.challenge { border: 2px solid #FFD600; background: #2A2410; }
        .achievement-fill { position: absolute; top: 0; left: 0; bottom: 0; background: rgba(0, 191, 165, 0.1); transition: width 0.5s; }
        .icon { width: 56px; height: 56px; margin-right: 15px; background: #2A2E45; border-radius: 6px; z-index: 1; }
        .info { flex-grow: 1; min-width: 0; z-index: 1; }
        .title { font-size: 16px; font-weight: bold; margin: 0; color: #FFF; }
        .desc { font-size: 12px; color: #B0B0B0; margin: 4px 0 0 0; line-height: 1.3; }
        .achievement-footer { display: flex; align-items: center; justify-content: space-between; margin-top: 6px; }
        .points { font-size: 11px; color: #FFD600; font-weight: 800; }
        .step-progress { font-size: 11px; color: #00BFA5; font-weight: bold; }
        .badge-pill { position: absolute; top: 0; right: 0; font-size: 9px; font-weight: 900; padding: 1px 8px; border-bottom-left-radius: 6px; text-transform: uppercase; z-index: 2; color: #000; }
        .badge-missable { background: #FF5252; color: #FFF; }
        .badge-progression { background: #00BFA5; }
        .badge-win { background: #FFD600; }
        .badge-challenge { border: 1px solid #FFD600; background: rgba(255, 214, 0, 0.2); color: #FFD600; }
        </style><script>
        function formatTemp(t) { return t > 0 ? (t/1000).toFixed(1) + '°' : '--'; }
        function update(data) {
          if(!data) return;
          if(data.game_title) document.getElementById('game-title').innerText = data.game_title;
          if(data.fps !== undefined) document.getElementById('fps').innerText = Math.round(data.fps);
          if(data.frametime !== undefined) document.getElementById('frametime').innerText = data.frametime ? data.frametime.toFixed(1) + 'ms' : '--';
          if(data.cpu_util !== undefined) document.getElementById('cpu_util').innerText = Math.round(data.cpu_util) + '%';
          if(data.gpu_util !== undefined) document.getElementById('gpu_util').innerText = Math.round(data.gpu_util) + '%';
          if(data.power_w !== undefined) document.getElementById('power_w').innerText = data.power_w ? data.power_w.toFixed(1) + 'W' : '--';
          if(data.temp_cpu !== undefined) document.getElementById('temp_cpu').innerText = formatTemp(data.temp_cpu);
          if(data.temp_gpu !== undefined) document.getElementById('temp_gpu').innerText = formatTemp(data.temp_gpu);
          if(data.battery !== undefined) document.getElementById('battery').innerText = data.battery + '%';
          if (data.achievements) {
            const total = data.achievements.length; const unlocked = data.achievements.filter(a => a.unlocked).length;
            const percent = total > 0 ? Math.round((unlocked / total) * 100) : 0;
            document.getElementById('progress-text').innerText = unlocked + ' / ' + total + ' (' + percent + '%)';
            document.getElementById('progress-fill').style.width = percent + '%';
            const list = document.getElementById('achievement-list');
            let html = ''; data.achievements.forEach(a => {
              const statusClass = a.unlocked ? 'unlocked' : (a.is_challenge ? 'challenge' : 'locked');
              const fillWidth = a.unlocked ? 100 : (a.progress_percent || 0);
              
              // Ensure we show progress text if it exists, even if it's just a number
              const rawProgress = a.progress_text !== undefined ? a.progress_text.toString() : '';
              const hasProgress = rawProgress.trim() !== '';
              
              let typeBadge = '';
              if (a.is_challenge) typeBadge = '<div class="badge-pill badge-challenge">Active Challenge</div>';
              else if (a.type === 1) typeBadge = '<div class="badge-pill badge-missable">Missable</div>';
              else if (a.type === 2) typeBadge = '<div class="badge-pill badge-progression">Progression</div>';
              else if (a.type === 3) typeBadge = '<div class="badge-pill badge-win">Win Condition</div>';
              
              html += '<div class="achievement ' + statusClass + '\">' + typeBadge +
                      '<div class="achievement-fill" style="width:' + fillWidth + '%"></div>' + '<img class="icon" src="' + (a.unlocked ? a.badge_url : a.badge_locked_url) + '">' +
                      '<div class="info"><p class="title">' + a.title + '</p><p class="desc">' + a.description + '</p><div class="achievement-footer">' +
                      '<span class="points">🪙 ' + a.points + ' Points</span>' + (hasProgress ? '<span class="step-progress">' + rawProgress + '</span>' : '') + '</div></div></div>';
            }); list.innerHTML = html;
          }
        } setInterval(() => { const now = new Date(); document.getElementById('clock').innerText = String(now.getHours()).padStart(2,'0') + ':' + String(now.getMinutes()).padStart(2,'0'); }, 1000);
        </script></head><body>
        <div class='dashboard'><p class='game-title' id='game-title'>Waiting...</p>
        <div class='telemetry-grid'>
            <div class='telemetry-row'>
                <div class='column'><span class='val-text' id='frametime'>--ms</span><span class='val-text' id='fps'>--</span></div>
                <div class='column'><span class='val-text' id='cpu_util'>--%</span><span class='val-text' id='temp_cpu'>--°</span></div>
                <div class='column'><span class='val-text' id='gpu_util'>--%</span><span class='val-text' id='temp_gpu'>--°</span></div>
                <div class='column'><span class='val-text' id='battery'>--%</span><span class='val-text' id='power_w'>--W</span></div>
            </div>
            <div class='telemetry-row'>
                <span class='label'>FRAME</span><span class='label'>CPU</span><span class='label'>GPU</span><span class='label'>BATT</span>
            </div>
        </div>
        <div class='anchored-row'>
            <div class='game-progress' id='progress-text'>-- / --</div>
            <div class='clock-container'><span class='clock' id='clock'>--:--</span></div>
        </div>
        <div class='progress-bar-bg'><div class='progress-bar-fill' id='progress-fill'></div></div></div>
        <div class='content' id='achievement-list'></div></body></html>"""
    }
}
