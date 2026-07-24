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
    private var webView: WebView? = null
    private var running = true
    private var socket: DatagramSocket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Passive HUD Mode: Prevent focus and interaction from controllers/keys
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        
        val wv = WebView(this)
        webView = wv
        setContentView(wv)
        wv.settings.javaScriptEnabled = true
        wv.setBackgroundColor(Color.BLACK)
        wv.loadDataWithBaseURL("https://retroarch.dual", DASHBOARD_HTML, "text/html", "UTF-8", null)
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
                        if (running && !isFinishing && !isDestroyed) {
                            webView?.evaluateJavascript("update($json);", null)
                        }
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
        webView = null
        super.onDestroy()
    }

    companion object {
        private val DASHBOARD_HTML = """<html><head><style>
        body { background-color: #0F111A; color: #E0E0E0; font-family: sans-serif; padding: 0; margin: 0; overflow: hidden; }
        .dashboard { position: fixed; top: 0; left: 0; right: 0; height: 120px; background: #1A1C2E; display: flex; flex-direction: column; padding: 8px 0 0 0; border-bottom: 4px solid #00BFA5; z-index: 100; box-sizing: border-box; }
        .game-title { font-size: 19px; font-weight: 800; color: #00BFA5; margin: 0 0 2px 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; width: 100%; text-align: center; }
        .settings-btn { position: absolute; top: 10px; right: 10px; color: #888; cursor: pointer; z-index: 110; padding: 5px; background: rgba(0,0,0,0.3); border-radius: 4px; }
        .settings-btn svg { width: 16px; height: 16px; fill: currentColor; display: block; }
        
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
        
        .subset-header { font-size: 13px; font-weight: 900; color: #00BFA5; text-transform: uppercase; margin: 15px 0 8px 0; padding-bottom: 4px; border-bottom: 1px solid #2A2E45; letter-spacing: 1px; display: flex; justify-content: space-between; align-items: center; }
        .subset-count { font-size: 10px; color: #888; }
        
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

        /* Filter Modal */
        #modal-overlay { display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.8); z-index: 200; align-items: center; justify-content: center; }
        .modal { background: #1A1C2E; width: 80%; max-width: 400px; border: 1px solid #00BFA5; border-radius: 12px; padding: 20px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
        .modal-title { font-size: 18px; font-weight: bold; color: #00BFA5; margin-bottom: 20px; border-bottom: 2px solid #00BFA5; padding-bottom: 10px; }
        .filter-list { max-height: 300px; overflow-y: auto; }
        .filter-item { display: flex; align-items: center; padding: 12px 0; border-bottom: 1px solid #2A2E45; cursor: pointer; }
        .filter-item:last-child { border-bottom: none; }
        .filter-item input { margin-right: 15px; transform: scale(1.5); }
        .filter-item span { font-size: 14px; font-weight: bold; flex: 1; }
        .modal-divider { height: 1px; background: #2A2E45; margin: 15px 0; }
        .modal-close { margin-top: 20px; width: 100%; padding: 12px; background: #00BFA5; border: none; border-radius: 6px; color: #000; font-weight: 900; text-transform: uppercase; cursor: pointer; }
        </style><script>
        function getInitialState() {
          return {
            game_title: 'RetroArch Lnk',
            fps: '--',
            frametime: '--ms',
            cpu_util: '--%',
            gpu_util: '--%',
            power_w: '--W',
            temp_cpu: '--°',
            temp_gpu: '--°',
            battery: '--%',
            achievements: [],
            activeSubsets: {},
            showHeaders: false
          };
        }

        let state = getInitialState();

        function formatTemp(t) { return t > 0 ? (t/1000).toFixed(1) + '°' : '--°'; }

        function update(newData) {
          if(!newData) return;

          if (newData.game_title && newData.game_title !== state.game_title) {
            const title = newData.game_title;
            const prevHeaders = state.showHeaders;
            state = getInitialState();
            state.game_title = title;
            state.showHeaders = prevHeaders;
          }

          // Handle alternative telemetry keys (PPSSPP uses 'cpu', 'gpu', 'power', etc.)
          const cpuVal = newData.cpu_util ?? newData.cpu_percent ?? newData.cpu;
          const gpuVal = newData.gpu_util ?? newData.gpu_percent ?? newData.gpu;
          const batteryVal = newData.battery ?? newData.batt_percent ?? newData.battery_percent;
          const powerVal = newData.power_w ?? newData.power;
          
          if(newData.fps !== undefined) state.fps = Math.round(newData.fps);
          if(newData.frametime !== undefined) state.frametime = newData.frametime.toFixed(1) + 'ms';
          if(cpuVal !== undefined) state.cpu_util = Math.round(cpuVal) + '%';
          if(gpuVal !== undefined) state.gpu_util = Math.round(gpuVal) + '%';
          if(powerVal !== undefined) state.power_w = powerVal.toFixed(1) + 'W';
          if(newData.temp_cpu !== undefined) state.temp_cpu = formatTemp(newData.temp_cpu);
          if(newData.temp_gpu !== undefined) state.temp_gpu = formatTemp(newData.temp_gpu);
          if(batteryVal !== undefined) state.battery = batteryVal + '%';

          if (newData.achievements) {
            newData.achievements.forEach(newA => {
              const sId = newA.subset_id || 0;
              const sTitle = newA.subset_title || '';
              
              if (state.activeSubsets[sId] === undefined) {
                const isBase = (sId === 0);
                const matchesGame = (sTitle.trim() === state.game_title.trim());
                state.activeSubsets[sId] = isBase || matchesGame;
              }
              
              const idx = state.achievements.findIndex(a => a.title === newA.title);
              if (idx !== -1) {
                state.achievements[idx] = { ...state.achievements[idx], ...newA };
              } else {
                state.achievements.push(newA);
              }
            });
          }

          render();
        }

        function toggleSubset(id) {
          state.activeSubsets[id] = !state.activeSubsets[id];
          render();
          renderSettings();
        }

        function toggleHeaders() {
          state.showHeaders = !state.showHeaders;
          render();
          renderSettings();
        }

        function toggleSettings(show) {
          document.getElementById('modal-overlay').style.display = show ? 'flex' : 'none';
          if (show) renderSettings();
        }

        function renderSettings() {
          const container = document.getElementById('filter-list');
          const subsetsMap = {};
          state.achievements.forEach(a => {
            const title = a.subset_title || 'Base Set';
            const id = a.subset_id || 0;
            if (!subsetsMap[id]) subsetsMap[id] = title;
          });

          let html = '';
          
          html += '<div class="filter-item" onclick="toggleHeaders()">' +
                  '<input type="checkbox" ' + (state.showHeaders ? 'checked' : '') + ' onclick="event.stopPropagation(); toggleHeaders()">' +
                  '<span>Show Subset Headers</span></div>';
          
          html += '<div class="modal-divider"></div>';

          Object.keys(subsetsMap).sort((a,b) => a-b).forEach(id => {
            const active = state.activeSubsets[id];
            const title = subsetsMap[id];
            html += '<div class="filter-item" onclick="toggleSubset(' + id + ')">' +
                    '<input type="checkbox" ' + (active ? 'checked' : '') + ' onclick="event.stopPropagation(); toggleSubset(' + id + ')">' +
                    '<span>' + title + '</span></div>';
          });
          container.innerHTML = html;
        }

        function render() {
          document.getElementById('game-title').innerText = state.game_title;
          document.getElementById('fps').innerText = state.fps;
          document.getElementById('frametime').innerText = state.frametime;
          document.getElementById('cpu_util').innerText = state.cpu_util;
          document.getElementById('gpu_util').innerText = state.gpu_util;
          document.getElementById('power_w').innerText = state.power_w;
          document.getElementById('temp_cpu').innerText = state.temp_cpu;
          document.getElementById('temp_gpu').innerText = state.temp_gpu;
          document.getElementById('battery').innerText = state.battery;

          const visibleAchievements = state.achievements.filter(a => state.activeSubsets[a.subset_id || 0]);
          
          const total = visibleAchievements.length;
          const unlocked = visibleAchievements.filter(a => a.unlocked).length;
          const percent = total > 0 ? Math.round((unlocked / total) * 100) : 0;
          document.getElementById('progress-text').innerText = unlocked + ' / ' + total + ' (' + percent + '%)';
          document.getElementById('progress-fill').style.width = percent + '%';

          const list = document.getElementById('achievement-list');
          let html = '';
          
          const subsetsMap = {};
          visibleAchievements.forEach(a => {
            const title = a.subset_title || 'Base Set';
            const id = a.subset_id || 0;
            if (!subsetsMap[id]) {
              subsetsMap[id] = { title: title, items: [] };
            }
            subsetsMap[id].items.push(a);
          });

          const sortedIds = Object.keys(subsetsMap).sort((a, b) => a - b);

          sortedIds.forEach(id => {
            const subset = subsetsMap[id];
            const unlockedCount = subset.items.filter(i => i.unlocked).length;
            
            if (state.showHeaders) {
              html += '<div class="subset-header">' +
                      '<span>' + subset.title + '</span>' +
                      '<span class="subset-count">' + unlockedCount + ' / ' + subset.items.length + '</span>' +
                      '</div>';
            }

            subset.items.forEach(a => {
              const statusClass = a.unlocked ? 'unlocked' : (a.is_challenge ? 'challenge' : 'locked');
              const fillWidth = a.unlocked ? 100 : (a.progress_percent || 0);
              const progressDisplay = (a.progress_text && a.progress_text.toString().trim() !== '') ? a.progress_text : '';
              
              let typeBadge = '';
              if (a.is_challenge) typeBadge = '<div class="badge-pill badge-challenge">Active Challenge</div>';
              else if (a.type === 1) typeBadge = '<div class="badge-pill badge-missable">Missable</div>';
              else if (a.type === 2) typeBadge = '<div class="badge-pill badge-progression">Progression</div>';
              else if (a.type === 3) typeBadge = '<div class="badge-pill badge-win">Win Condition</div>';

              html += '<div class="achievement ' + statusClass + '\">' + typeBadge +
                      '<div class="achievement-fill" style="width:' + fillWidth + '%"></div>' + 
                      '<img class="icon" src="' + (a.unlocked ? a.badge_url : a.badge_locked_url) + '">' +
                      '<div class="info"><p class="title">' + a.title + '</p><p class="desc">' + a.description + '</p><div class="achievement-footer">' +
                      '<span class="points">🪙 ' + a.points + ' Points</span>' + 
                      (progressDisplay ? '<span class="step-progress">' + progressDisplay + '</span>' : '') + 
                      '</div></div></div>';
            });
          });

          list.innerHTML = html;
        }

        setInterval(() => {
          const now = new Date();
          document.getElementById('clock').innerText = String(now.getHours()).padStart(2,'0') + ':' + String(now.getMinutes()).padStart(2,'0');
        }, 1000);
        </script></head><body>
        <div id="modal-overlay" onclick="toggleSettings(false)">
            <div class="modal" onclick="event.stopPropagation()">
                <div class="modal-title">Display Settings</div>
                <div id="filter-list" class="filter-list"></div>
                <button class="modal-close" onclick="toggleSettings(false)">Close</button>
            </div>
        </div>
        <div class='dashboard'>
            <div class="settings-btn" onclick="toggleSettings(true)">
                <svg viewBox="0 0 24 24"><path d="M19.14,12.94c0.04-0.3,0.06-0.61,0.06-0.94c0-0.32-0.02-0.64-0.07-0.94l2.03-1.58c0.18-0.14,0.23-0.41,0.12-0.61 l-1.92-3.32c-0.12-0.22-0.37-0.29-0.59-0.22l-2.39,0.96c-0.5-0.38-1.03-0.7-1.62-0.94L14.4,2.81c-0.04-0.24-0.24-0.41-0.48-0.41 h-3.84c-0.24,0-0.43,0.17-0.47,0.41L9.25,5.35C8.66,5.59,8.12,5.92,7.63,6.29L5.24,5.33c-0.22-0.08-0.47,0-0.59,0.22L2.74,8.87 C2.62,9.08,2.66,9.34,2.86,9.48l2.03,1.58C4.84,11.36,4.8,11.69,4.8,12s0.02,0.64,0.07,0.94l-2.03,1.58 c-0.18,0.14-0.23,0.41-0.12,0.61l1.92,3.32c0.12,0.22,0.37,0.29,0.59,0.22l2.39-0.96c0.5,0.38,1.03,0.7,1.62,0.94l0.36,2.54 c0.05,0.24,0.24,0.41,0.48,0.41h3.84c0.24,0,0.44-0.17,0.47-0.41l0.36-2.54c0.59-0.24,1.13-0.56,1.62-0.94l2.39,0.96 c0.22,0.08,0.47,0,0.59-0.22l1.92-3.32c0.12-0.22,0.07-0.47-0.12-0.61L19.14,12.94z M12,15.6c-1.98,0-3.6-1.62-3.6-3.6 s1.62-3.6,3.6-3.6s3.6,1.62,3.6,3.6S13.98,15.6,12,15.6z"/></svg>
            </div>
            <p class='game-title' id='game-title'>Waiting...</p>
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
            <div class='progress-bar-bg'><div class='progress-bar-fill' id='progress-fill'></div></div>
        </div>
        <div class='content' id='achievement-list'></div></body></html>"""
    }
}
