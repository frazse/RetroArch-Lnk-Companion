package com.example.retroachivementscompanion

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import android.webkit.JavascriptInterface
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
        setPassiveMode(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val wv = WebView(this)
        webView = wv
        setContentView(wv)
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.settings.databaseEnabled = true
        wv.setBackgroundColor(Color.BLACK)
        wv.addJavascriptInterface(WebAppInterface(), "Android")
        wv.loadDataWithBaseURL("https://retroarch.dual", DASHBOARD_HTML, "text/html", "UTF-8", null)
        startUdpListener()
    }

    private fun setPassiveMode(passive: Boolean) {
        runOnUiThread {
            if (passive) window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        }
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun setFocusable(focusable: Boolean) { setPassiveMode(!focusable) }
    }

    private fun startUdpListener() {
        Thread {
            try {
                val s = DatagramSocket(55432)
                socket = s
                val buffer = ByteArray(65535)
                while (running) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    s.receive(packet)
                    val json = String(packet.data, 0, packet.length)
                    runOnUiThread { if (running && !isFinishing && !isDestroyed) webView?.evaluateJavascript("update($json);", null) }
                }
            } catch (e: Exception) { if (running) Log.e("RetroArchLnk", "Socket error", e) }
            finally { socket?.close(); socket = null }
        }.start()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val isPassive = (window.attributes.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) != 0
        return if (isPassive) true else super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val isPassive = (window.attributes.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) != 0
        return if (isPassive) true else super.dispatchGenericMotionEvent(event)
    }

    override fun onDestroy() { running = false; socket?.close(); webView = null; super.onDestroy() }

    companion object {
        private val DASHBOARD_HTML = """<html><head><style>
        body { background-color: #0F111A; color: #E0E0E0; font-family: sans-serif; padding: 0; margin: 0; overflow: hidden; }
        .settings-btn { position: fixed; top: 5px; right: 5px; color: #888; cursor: pointer; z-index: 1000; padding: 4px; background: rgba(26,28,46,0.8); border-radius: 6px; border: 1px solid #2A2E45; }
        .settings-btn svg { width: 18px; height: 18px; fill: currentColor; display: block; }
        .dashboard { position: fixed; top: 0; left: 0; right: 0; height: 120px; background: #1A1C2E; display: flex; flex-direction: column; padding: 8px 0 0 0; border-bottom: 4px solid #00BFA5; z-index: 100; box-sizing: border-box; transition: transform 0.4s; }
        .dashboard.hidden { transform: translateY(-100%); }
        .game-title { font-size: 19px; font-weight: 800; color: #00BFA5; margin: 0 0 2px 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; width: 100%; text-align: center; }
        .telemetry-grid { display: flex; flex-direction: column; flex: 1; justify-content: center; }
        .telemetry-row { display: flex; justify-content: space-evenly; width: 100%; }
        .column { display: flex; flex-direction: column; align-items: center; width: 25%; }
        .val-text { font-size: 18px; font-weight: bold; color: #FFF; line-height: 1.0; margin-bottom: 2px; transition: none !important; }
        .label { font-size: 10px; color: #888; font-weight: 900; text-transform: uppercase; letter-spacing: 0.5px; width: 25%; text-align: center; margin-top: 2px; }
        .anchored-row { position: relative; width: 100%; height: 20px; display: flex; align-items: center; margin-top: auto; }
        .game-progress { position: absolute; left: 10px; font-size: 12px; color: #888; font-weight: 900; }
        .session-timer { position: absolute; left: 10px; top: 10px; font-size: 12px; font-weight: 900; color: #888; }
        .clock-container { position: absolute; right: 10px; }
        .clock { font-size: 12px; font-weight: 900; color: #888; }
        .progress-bar-bg { width: 100%; height: 8px; background: #2A2E45; overflow: hidden; }
        .progress-bar-fill { height: 100%; background: #4CAF50; width: 0%; transition: width 0.5s; }
        .wrapper { margin-top: 120px; height: calc(100vh - 120px); display: flex; flex-direction: column; overflow: hidden; width: 100%; transition: margin-top 0.4s, height 0.4s; }
        .wrapper.full-screen { margin-top: 0; height: 100vh; }
        #pinned-achievements { flex-shrink: 0; background: #0F111A; padding: 18px 18px 0 18px; box-sizing: border-box; border-bottom: 2px solid #2A2E45; display: none; }
        #pinned-achievements:not(:empty) { display: block; }
        .content { flex-grow: 1; overflow-y: auto; padding: 18px; box-sizing: border-box; width: 100%; }
        #achievement-list { width: 100%; display: block; }
        .subset-header { font-size: 13px; font-weight: 900; color: #00BFA5; text-transform: uppercase; margin: 15px 0 8px 0; padding-bottom: 4px; border-bottom: 1px solid #2A2E45; letter-spacing: 1px; display: flex; justify-content: space-between; align-items: center; width: 100%; }
        .subset-header.completed { color: #888; border-bottom-color: #1E2132; margin-top: 25px; }
        .achievement { display: flex; align-items: flex-start; margin-bottom: 12px; padding: 12px; background: #1E2132; border-radius: 10px; border: 1px solid #2A2E45; position: relative; overflow: hidden; transition: border-color 0.3s, opacity 0.3s, transform 0.3s, box-shadow 0.3s; width: 100%; box-sizing: border-box; }
        .achievement.unlocked { border-left: 4px solid #4CAF50; background: #242938; opacity: 0.6; }
        .achievement.challenge { border: 2px solid #FFD600; background: #2A2410; }
        .achievement-fill { position: absolute; top: 0; left: 0; bottom: 0; background: rgba(0, 191, 165, 0.1); transition: width 0.5s; z-index: 0; }
        .icon { width: 56px; height: 56px; margin-right: 15px; background: #2A2E45; border-radius: 6px; z-index: 2; flex-shrink: 0; }
        .info { flex: 1; min-width: 0; z-index: 2; display: flex; flex-direction: column; }
        .title { font-size: 16px; font-weight: bold; margin: 0; color: #FFF; line-height: 1.2; }
        .desc { font-size: 12px; color: #B0B0B0; margin: 4px 0 0 0; line-height: 1.3; }
        .achievement-footer { display: flex; align-items: center; justify-content: space-between; margin-top: 8px; }
        .points { font-size: 11px; color: #FFD600; font-weight: 800; display: flex; align-items: center; gap: 4px; }
        .step-progress { font-size: 11px; color: #00BFA5; font-weight: bold; }
        .badge-pill { position: absolute; top: 0; right: 0; font-size: 9px; font-weight: 900; padding: 1px 8px; border-bottom-left-radius: 6px; text-transform: uppercase; z-index: 2; color: #000; }
        .badge-missable { background: #FF5252; color: #FFF; }
        .badge-progression { background: #00BFA5; }
        .badge-win { background: #FFD600; }
        .badge-challenge { border: 1px solid #FFD600; background: rgba(255, 214, 0, 0.2); color: #FFD600; }
        .profile-banner { display: flex; padding: 15px; background: #1A1C2E; border: 1px solid #00BFA5; border-radius: 12px; margin-bottom: 15px; width: 100%; box-sizing: border-box; gap: 15px; }
        .profile-left { display: flex; align-items: center; gap: 15px; flex: 1; }
        .avatar { width: 56px; height: 56px; border-radius: 50%; border: 2px solid #00BFA5; flex-shrink: 0; }
        .profile-info { flex: 1; }
        .profile-name { font-size: 18px; font-weight: 800; color: #FFF; margin-bottom: 2px; }
        .profile-stats { font-size: 11px; color: #888; }
        .profile-stats strong { color: #00BFA5; }
        .retropoints-text { color: #FFD600; }
        .profile-right { display: flex; flex-direction: column; justify-content: center; align-items: flex-end; padding-left: 15px; border-left: 1px solid #2A2E45; gap: 4px; min-width: 130px; }
        .award-stat { display: flex; align-items: center; gap: 6px; font-size: 9px; font-weight: 900; text-transform: uppercase; color: #888; }
        .award-stat span { font-size: 13px; color: #FFF; min-width: 18px; text-align: right; }
        .prog-circle { width: 8px; height: 8px; border-radius: 50%; }
        .circle-beaten { background: #FFF; box-shadow: 0 0 5px #FFF; }
        .circle-mastered { background: #FFD600; box-shadow: 0 0 5px #FFD600; }
        .circle-soft-beaten { border: 1px solid #888; }
        .aotw-card { background: linear-gradient(135deg, #1A1C2E 0%, #2A2E45 100%); border: 2px solid #FFD600; border-radius: 10px; padding: 12px; margin-bottom: 15px; position: relative; width: 100%; box-sizing: border-box; }
        .aotw-label { position: absolute; top: -10px; left: 10px; background: #FFD600; color: #000; font-size: 9px; font-weight: 900; padding: 2px 8px; border-radius: 4px; text-transform: uppercase; }
        .game-card { padding: 15px; background: #1E2132; border-radius: 10px; border: 1px solid #2A2E45; margin-bottom: 12px; cursor: pointer; width: 100%; box-sizing: border-box; }
        .game-card-header { display: flex; align-items: flex-start; width: 100%; }
        .game-icon { width: 56px; height: 56px; margin-right: 15px; border-radius: 6px; flex-shrink: 0; }
        .game-info { flex: 1; min-width: 0; display: flex; flex-direction: column; }
        .game-meta-row { display: flex; justify-content: space-between; align-items: baseline; width: 100%; }
        .game-title-text { font-size: 17px; font-weight: bold; color: #FFF; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; flex: 1; padding-right: 10px; }
        .game-console { font-size: 10px; color: #00BFA5; font-weight: 900; text-transform: uppercase; margin-bottom: 6px; }
        .game-stats-text { font-size: 12px; color: #888; font-weight: bold; }
        .game-progress-bar-bg { height: 8px; background: #2A2E45; border-radius: 4px; overflow: hidden; margin-top: 6px; width: 100%; }
        .game-progress-bar-fill { height: 100%; background: #00BFA5; }
        .expanded-achievements { margin-top: 15px; padding-top: 15px; border-top: 1px solid #2A2E45; display: none; }
        .expanded-achievements.active { display: block; }
        #modal-overlay { display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.8); z-index: 2000; align-items: center; justify-content: center; }
        .modal { background: #1A1C2E; width: 90%; max-width: 450px; border: 1px solid #00BFA5; border-radius: 12px; padding: 20px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); display: flex; flex-direction: column; max-height: 90vh; }
        .input-group input { width: 100%; background: #0F111A; border: 1px solid #2A2E45; color: #FFF; padding: 10px; border-radius: 6px; box-sizing: border-box; }
        .btn-save { width: 100%; padding: 12px; background: #00BFA5; border: none; border-radius: 6px; color: #000; font-weight: 900; text-transform: uppercase; cursor: pointer; }
        .filter-item { display: flex; align-items: center; padding: 12px 0; border-bottom: 1px solid #2A2E45; cursor: pointer; }
        .filter-item input { margin-right: 15px; transform: scale(1.5); }
        .filter-item span { font-size: 14px; font-weight: bold; flex: 1; }
        .modal-divider { height: 1px; background: #2A2E45; margin: 15px 0; flex-shrink: 0; }
        ::-webkit-scrollbar { width: 4px; }
        ::-webkit-scrollbar-thumb { background: #2A2E45; border-radius: 10px; }
        </style><script>
        var state = { game_title: 'No Game Running', fps: '--', cpu_util: '--%', gpu_util: '--%', battery: '--%', temp_cpu: '--°', temp_gpu: '--°', frametime: '--ms', power_w: '--W', achievements: [], activeSubsets: {}, showHeaders: false, hideUnlocked: false, recentUnlocks: {}, startTime: Date.now(), lastPacketTime: 0, profile: null, recentGames: [], aotw: null, awardCounts: { beaten: 0, mastered: 0, softBeaten: 0, softCompleted: 0 }, expandedGame: null, gameAchievements: {}, isLoading: false, profilePicUrl: '', isApiActive: false, lastApiPoll: 0, currentRpGameId: null, lastRpAchievementFetch: 0 };
        var nextGlobalIndex = 0;
        
        async function apiFetch(endpoint, params) {
          var user = window.localStorage.getItem('ra_user'); var key = window.localStorage.getItem('ra_key');
          if (!user || !key) return null;
          var url = 'https://retroachievements.org/API/' + endpoint + '?z=' + user + '&y=' + key;
          if (params) for (var p in params) url += '&' + p + '=' + params[p];
          try { var res = await fetch(url); return await res.json(); } catch(e) { return null; }
        }

        async function fetchProfile() {
          var user = window.localStorage.getItem('ra_user'); if(!user) { render(); return; }
          state.isLoading = true; render();
          var sum = await apiFetch('API_GetUserSummary.php', { u: user, g: 0, a: 0 }); 
          if (sum && sum.User) {
            state.profile = sum;
            state.profilePicUrl = 'https://retroachievements.org' + sum.UserPic + '?t=' + Date.now();
          }
          var awards = await apiFetch('API_GetUserAwards.php', { u: user });
          if (awards) {
            state.awardCounts = { beaten: awards.BeatenHardcoreAwardsCount || 0, softBeaten: awards.BeatenSoftcoreAwardsCount || 0, mastered: awards.MasteryAwardsCount || 0, softCompleted: awards.CompletionAwardsCount || 0 };
          }
          var games = await apiFetch('API_GetUserRecentlyPlayedGames.php', { u: user, c: 5 }); if (Array.isArray(games)) state.recentGames = games;
          var aotw = await apiFetch('API_GetAchievementOfTheWeek.php'); if (aotw) state.aotw = aotw;
          state.isLoading = false; render();
        }

        async function richPresencePoll() {
          var syncEnabled = window.localStorage.getItem('ra_sync_rp') === 'true';
          if (!syncEnabled) { state.isApiActive = false; return; }
          if (Date.now() - state.lastPacketTime < 15000) return;
          if (Date.now() - state.lastApiPoll < 30000) return;
          state.lastApiPoll = Date.now();
          
          var user = window.localStorage.getItem('ra_user');
          var sum = await apiFetch('API_GetUserSummary.php', { u: user, g: 1, a: 0 });
          
          if (sum && sum.LastGameID && sum.LastGameID !== "0" && sum.RichPresenceMsg && sum.RichPresenceMsg.toLowerCase().indexOf('in menus') === -1) {
             var isPlaying = false;
             if (sum.RecentlyPlayed && sum.RecentlyPlayed.length > 0 && sum.RecentlyPlayed[0].GameID == sum.LastGameID) {
                var rpDate = new Date(sum.RichPresenceMsgDate + ' UTC');
                var ageMs = Date.now() - rpDate.getTime();
                if (ageMs < 120000) isPlaying = true;
             }

             if (isPlaying) {
                state.isApiActive = true;
                state.game_title = sum.LastGame ? sum.LastGame.Title : state.game_title;
                
                if (state.currentRpGameId !== sum.LastGameID || (Date.now() - state.lastRpAchievementFetch >= 30000)) {
                   state.currentRpGameId = sum.LastGameID;
                   state.lastRpAchievementFetch = Date.now();
                   var data = await apiFetch('API_GetGameInfoAndUserProgress.php', { u: user, g: sum.LastGameID });
                   if (data && data.Achievements) {
                      state.activeSubsets = { 0: true };
                      var oldAchievements = state.achievements;
                      state.achievements = Object.values(data.Achievements).map(function(a, idx) {
                         var isUnlocked = !!(a.DateEarnedHardcore || a.DateEarned);
                         var oldA = oldAchievements.find(function(o){ return o.title === a.Title; });
                         if (oldA && !oldA.unlocked && isUnlocked) {
                            state.recentUnlocks[a.Title] = Date.now();
                            setTimeout(function(){ delete state.recentUnlocks[a.Title]; render(); }, 10000);
                            fetchProfile();
                         }
                         return { title: a.Title, description: a.Description, points: a.Points, unlocked: isUnlocked, BadgeName: a.BadgeName, subset_id: 0, originalIndex: idx };
                      });
                   }
                }
             } else { state.isApiActive = false; state.currentRpGameId = null; }
          } else { state.isApiActive = false; state.currentRpGameId = null; }
          render();
        }

        async function toggleGameExpansion(gameId) {
          if (state.expandedGame === gameId) { state.expandedGame = null; } else {
            state.expandedGame = gameId;
            if (!state.gameAchievements[gameId]) {
              var user = window.localStorage.getItem('ra_user');
              var data = await apiFetch('API_GetGameInfoAndUserProgress.php', { u: user, g: gameId });
              if (data && data.Achievements) {
                state.gameAchievements[gameId] = Object.values(data.Achievements).filter(function(a){return a.DateEarnedHardcore || a.DateEarned;})
                  .sort(function(a, b){return new Date(b.DateEarnedHardcore || b.DateEarned) - new Date(a.DateEarnedHardcore || a.DateEarned);}).slice(0, 5);
              }
            }
          }
          render();
        }

        function update(newData) {
          if(!newData || !newData.game_title) return;
          state.isApiActive = false; 
          var isGeneric = ['RetroArch','Dolphin','PPSSPP'].includes(newData.game_title);
          if (isGeneric) { state.lastPacketTime = 0; render(); return; }
          state.lastPacketTime = Date.now();
          if (newData.game_title !== state.game_title) {
            var old = state; state = { game_title: newData.game_title, fps: '--', cpu_util: '--%', gpu_util: '--%', battery: '--%', temp_cpu: '--°', temp_gpu: '--°', frametime: '--ms', power_w: '--W', achievements: [], activeSubsets: {}, showHeaders: old.showHeaders, hideUnlocked: old.hideUnlocked, recentUnlocks: {}, startTime: Date.now(), lastPacketTime: Date.now(), profile: old.profile, recentGames: old.recentGames, awardCounts: old.awardCounts, aotw: old.aotw, expandedGame: null, gameAchievements: {}, isLoading: false, gameMetadata: {}, profilePicUrl: old.profilePicUrl, isApiActive: false, lastApiPoll: old.lastApiPoll, currentRpGameId: null };
          }
          if(newData.fps !== undefined) state.fps = Math.round(newData.fps);
          if(newData.frametime !== undefined) state.frametime = newData.frametime.toFixed(1) + 'ms';
          var c = newData.cpu_util ?? newData.cpu_percent ?? newData.cpu;
          if(c !== undefined) state.cpu_util = Math.round(c) + '%';
          if(newData.temp_cpu !== undefined) state.temp_cpu = (newData.temp_cpu > 1000 ? (newData.temp_cpu/1000).toFixed(1) : newData.temp_cpu) + '°';
          var g = newData.gpu_util ?? newData.gpu_percent ?? newData.gpu;
          if(g !== undefined) state.gpu_util = Math.round(g) + '%';
          if(newData.temp_gpu !== undefined) state.temp_gpu = (newData.temp_gpu > 1000 ? (newData.temp_gpu/1000).toFixed(1) : newData.temp_gpu) + '°';
          var b = newData.battery ?? newData.batt_percent ?? newData.battery_percent;
          if(b !== undefined) state.battery = b + '%';
          if(newData.power_w !== undefined) state.power_w = newData.power_w.toFixed(1) + 'W';
          if (newData.achievements) {
            newData.achievements.forEach(function(newA){
              var sId = newA.subset_id || 0;
              if (state.activeSubsets[sId] === undefined) {
                 var matching = (newA.subset_title || '').trim().toLowerCase() === state.game_title.trim().toLowerCase();
                 state.activeSubsets[sId] = (sId === 0 || matching);
              }
              var idx = state.achievements.findIndex(function(a){return a.title === newA.title;});
              if (idx !== -1) {
                if (!state.achievements[idx].unlocked && newA.unlocked) { state.recentUnlocks[newA.title] = Date.now(); setTimeout(function(){ delete state.recentUnlocks[newA.title]; render(); }, 10000); fetchProfile(); }
                state.achievements[idx] = Object.assign({}, state.achievements[idx], newA);
              } else { newA.originalIndex = nextGlobalIndex++; state.achievements.push(newA); }
            });
          }
          render();
        }

        function getAchievementHtml(a, isFeed) {
          var statusClass = (a.unlocked || isFeed) ? 'unlocked' : (a.is_challenge ? 'challenge' : 'locked');
          var unlockClass = state.recentUnlocks[a.title] ? ' just-unlocked' : '';
          var fillWidth = (a.unlocked || isFeed) ? 100 : (a.progress_percent || 0);
          var titleText = isFeed ? a.Title : a.title;
          var descText = isFeed ? a.Description : a.description;
          var pointsVal = isFeed ? a.Points : a.points;
          var pTxt = (a.progress_text && a.progress_text.toString().trim() !== '') ? a.progress_text : '';
          var rPoints = (isFeed && a.TrueRatio) ? a.TrueRatio : '';
          var icon = a.badge_url || a.badge_locked_url;
          if (!icon && a.BadgeName) icon = 'https://retroachievements.org/Badge/' + a.BadgeName + (a.unlocked || isFeed ? '' : '_lock') + '.png';
          var badge = '';
          if (a.is_challenge) badge = '<div class="badge-pill badge-challenge">Challenge</div>';
          else if (a.type === 1) badge = '<div class="badge-pill badge-missable">Missable</div>';
          else if (a.type === 2) badge = '<div class="badge-pill badge-progression">Progression</div>';
          else if (a.type === 3) badge = '<div class="badge-pill badge-win">Win Condition</div>';
          return '<div class="achievement ' + statusClass + unlockClass + '">' + badge +
                  '<div class="achievement-fill" style="width:' + fillWidth + '%"></div>' + 
                  '<img class="icon" src="' + icon + '">' +
                  '<div class="info"><p class="title">' + titleText + '</p><p class="desc">' + descText + '</p>' +
                  '<div class="achievement-footer"><span class="points">🪙 ' + pointsVal + (rPoints ? ' (📈 ' + rPoints + ')' : '') + '</span>' +
                  (pTxt ? '<span class="step-progress">' + pTxt + '</span>' : '') + '</div>' +
                  '</div></div>';
        }

        function render() {
          var hasGame = (Date.now() - state.lastPacketTime < 10000) || state.isApiActive; 
          var dash = document.querySelector('.dashboard');
          var wrap = document.querySelector('.wrapper');
          var tele = document.querySelector('.telemetry-grid');
          
          if (hasGame) {
             dash.classList.remove('hidden'); wrap.classList.remove('full-screen');
             if (state.isApiActive) tele.style.display = 'none'; else tele.style.display = 'flex';
             document.getElementById('game-title').innerText = state.game_title;
             document.getElementById('fps').innerText = state.fps; document.getElementById('cpu_util').innerText = state.cpu_util; document.getElementById('gpu_util').innerText = state.gpu_util; document.getElementById('battery').innerText = state.battery;
             document.getElementById('temp_cpu').innerText = state.temp_cpu; document.getElementById('temp_gpu').innerText = state.temp_gpu; document.getElementById('frametime').innerText = state.frametime; document.getElementById('power_w').innerText = state.power_w;
          } else { dash.classList.add('hidden'); wrap.classList.add('full-screen'); }

          if (!hasGame) {
             var html = '';
             if (state.profile) {
               var awd = state.awardCounts;
               html += '<div class="profile-banner"><div class="profile-left"><img class="avatar" src="' + state.profilePicUrl + '">' +
                       '<div class="profile-info"><div class="profile-name">' + state.profile.User + '</div>' +
                       '<div class="profile-stats">Points: <strong>' + state.profile.TotalPoints + '</strong> <span class="retropoints-text">(' + (state.profile.TotalTruePoints || 0) + ')</span> | Rank: <strong>#' + state.profile.Rank + '</strong></div></div></div>' +
                       '<div class="profile-right">' +
                       '<div class="award-stat"><div class="prog-circle circle-soft-beaten"></div>SOFT <span>' + awd.softBeaten + '</span></div>' +
                       '<div class="award-stat"><div class="prog-circle circle-beaten"></div>BEATEN <span>' + awd.beaten + '</span></div>' +
                       '<div class="award-stat"><div class="prog-circle circle-mastered"></div>MASTERED <span>' + awd.mastered + '</span></div></div></div>';
               if (state.aotw) {
                 html += '<div class="aotw-card"><div class="aotw-label">Achievement of the Week</div>' +
                         '<div style="display:flex;align-items:center;margin-top:5px;">' +
                         '<img src="https://retroachievements.org/Badge/' + state.aotw.Achievement.BadgeName + '.png" style="width:40px;margin-right:12px;border-radius:4px;">' +
                         '<div style="flex:1;"><div style="font-size:10px;color:#00BFA5;font-weight:900;text-transform:uppercase;margin-bottom:2px;">' + (state.aotw.Console ? state.aotw.Console.Title : '') + '</div>' +
                         '<div style="font-size:14px;font-weight:bold;color:#FFD600;">' + state.aotw.Achievement.Title + '</div>' +
                         '<div style="font-size:10px;color:#888;">' + state.aotw.Game.Title + '</div></div></div></div>';
               }
               if (state.recentGames.length > 0) {
                 html += '<div class="subset-header"><span>Last 5 Games Played</span></div>';
                 state.recentGames.forEach(function(game){
                   var percent = Math.round((game.NumAchievedHardcore / game.NumPossibleAchievements) * 100);
                   var isExpanded = state.expandedGame === game.GameID;
                   html += '<div class="game-card" onclick="toggleGameExpansion(' + game.GameID + ')">' +
                           '<div class="game-card-header"><img class="game-icon" src="https://media.retroachievements.org' + game.ImageIcon + '">' +
                           '<div class="game-info"><div class="game-console">' + game.ConsoleName + '</div>' +
                           '<div class="game-meta-row"><div class="game-title-text">' + game.Title + '</div>' +
                           '<div class="game-stats-text">' + game.NumAchievedHardcore + ' / ' + game.NumPossibleAchievements + ' (' + percent + '%)</div></div>' +
                           '<div class="game-progress-bar-bg"><div class="game-progress-bar-fill" style="width: ' + percent + '%"></div></div></div></div>' +
                           '<div class="expanded-achievements ' + (isExpanded ? 'active' : '') + '">';
                   if (isExpanded && state.gameAchievements[game.GameID]) {
                     state.gameAchievements[game.GameID].forEach(function(a){ html += getAchievementHtml(a, true); });
                   } else if (isExpanded) { html += '<div style="font-size:10px;color:#666;text-align:center;">Syncing achievements...</div>'; }
                   html += '</div></div>';
                 });
               }
             } else if (state.isLoading) { html = '<div style="text-align:center; padding: 50px; color:#888;">Syncing profile...</div>'; }
             else { html = '<div style="text-align:center; padding: 50px; color:#888;">Enter credentials in settings.</div>'; }
             document.getElementById('achievement-list').innerHTML = html;
             return;
          }

          var vis = state.achievements.filter(function(a){return state.activeSubsets[a.subset_id || 0];});
          var finalVis = state.hideUnlocked ? vis.filter(function(a){return !a.unlocked || state.recentUnlocks[a.title];}) : vis;
          var pinned = finalVis.filter(function(a){return a.is_challenge || state.recentUnlocks[a.title];}).sort(function(a,b){return a.originalIndex - b.originalIndex;});
          var remaining = finalVis.filter(function(a){return !a.is_challenge && !state.recentUnlocks[a.title];}).sort(function(a,b){return a.originalIndex - b.originalIndex;});
          
          document.getElementById('pinned-achievements').innerHTML = pinned.map(function(a){return getAchievementHtml(a);}).join('');
          var mainHtml = ''; var subsets = {};
          remaining.filter(function(a){return !a.unlocked;}).forEach(function(a){
            var id = a.subset_id || 0; if(!subsets[id]) subsets[id] = { title: a.subset_title || 'Base Set', items: [] };
            subsets[id].items.push(a);
          });
          Object.keys(subsets).sort(function(a,b){return a-b;}).forEach(function(id){
            if (state.showHeaders) mainHtml += '<div class="subset-header"><span>' + subsets[id].title + '</span></div>';
            mainHtml += subsets[id].items.map(function(a){return getAchievementHtml(a);}).join('');
          });
          var unlocked = remaining.filter(function(a){return a.unlocked;});
          if (!state.hideUnlocked && unlocked.length > 0) {
            if (state.showHeaders) mainHtml += '<div class="subset-header completed"><span>Completed</span></div>';
            mainHtml += unlocked.map(function(a){return getAchievementHtml(a);}).join('');
          }
          document.getElementById('achievement-list').innerHTML = mainHtml;
          
          var tCnt = vis.length; var uCnt = vis.filter(function(i){return i.unlocked;}).length;
          var per = tCnt > 0 ? Math.round((uCnt / tCnt) * 100) : 0;
          document.getElementById('progress-text').innerText = uCnt + ' / ' + tCnt + ' (' + per + '%)';
          document.getElementById('progress-fill').style.width = per + '%';
          var d = Date.now() - state.startTime;
          document.getElementById('session-timer').innerText = Math.floor(d/60000).toString().padStart(2,'0') + ':' + Math.floor((d%60000)/1000).toString().padStart(2,'0');
        }

        setInterval(function(){ 
          var now = new Date(); 
          document.getElementById('clock').innerText = now.getHours().toString().padStart(2,'0') + ':' + now.getMinutes().toString().padStart(2,'0'); 
          render();
          richPresencePoll();
        }, 1000);

        window.onload = function() { fetchProfile(); };
        
        function toggleSyncRP() { var curr = window.localStorage.getItem('ra_sync_rp') === 'true'; window.localStorage.setItem('ra_sync_rp', !curr); renderSettings(); }
        function toggleSubset(id) { state.activeSubsets[id] = !state.activeSubsets[id]; render(); renderSettings(); }
        function toggleHeaders() { state.showHeaders = !state.showHeaders; render(); renderSettings(); }
        function toggleHideUnlocked() { state.hideUnlocked = !state.hideUnlocked; render(); renderSettings(); }
        
        function toggleSettings(show) {
          document.getElementById('modal-overlay').style.display = show ? 'flex' : 'none';
          if (window.Android) window.Android.setFocusable(show);
          if (show) {
            document.getElementById('input-user').value = window.localStorage.getItem('ra_user') || '';
            document.getElementById('input-key').value = window.localStorage.getItem('ra_key') || '';
            renderSettings();
          }
        }
        function renderSettings() {
          var container = document.getElementById('filter-list'); var subsetsMap = {};
          state.achievements.forEach(function(a){
            var title = a.subset_title || 'Base Set'; var id = a.subset_id || 0;
            if (!subsetsMap[id]) subsetsMap[id] = title;
          });
          var syncEnabled = window.localStorage.getItem('ra_sync_rp') === 'true';
          var html = '<div class="filter-item" onclick="toggleSyncRP()"><input type="checkbox" ' + (syncEnabled ? 'checked' : '') + ' onclick="event.stopPropagation(); toggleSyncRP()"><span>Sync with Rich Presence (Exp)</span></div>';
          html += '<div class="filter-item" onclick="toggleHeaders()"><input type="checkbox" ' + (state.showHeaders ? 'checked' : '') + ' onclick="event.stopPropagation(); toggleHeaders()"><span>Show Subset Headers</span></div>';
          html += '<div class="filter-item" onclick="toggleHideUnlocked()"><input type="checkbox" ' + (state.hideUnlocked ? 'checked' : '') + ' onclick="event.stopPropagation(); toggleHideUnlocked()"><span>Hide Unlocked</span></div>';
          html += '<div class="modal-divider"></div>';
          Object.keys(subsetsMap).sort(function(a,b){return a-b;}).forEach(function(id){
            html += '<div class="filter-item" onclick="toggleSubset(' + id + ')"><input type="checkbox" ' + (state.activeSubsets[id] ? 'checked' : '') + ' onclick="event.stopPropagation(); toggleSubset(' + id + ')"><span>' + subsetsMap[id] + '</span></div>';
          });
          container.innerHTML = html;
        }
        function saveCredentials() {
          window.localStorage.setItem('ra_user', document.getElementById('input-user').value);
          window.localStorage.setItem('ra_key', document.getElementById('input-key').value);
          fetchProfile(); toggleSettings(false);
        }
        </script></head><body>
        <div class="settings-btn" onclick="toggleSettings(true)"><svg viewBox="0 0 24 24"><path d="M19.14,12.94c0.04-0.3,0.06-0.61,0.06-0.94c0-0.32-0.02-0.64-0.07-0.94l2.03-1.58c0.18-0.14,0.23-0.41,0.12-0.61 l-1.92-3.32c-0.12-0.22-0.37-0.29-0.59-0.22l-2.39,0.96c-0.5-0.38-1.03-0.7-1.62-0.94L14.4,2.81c-0.04-0.24-0.24-0.41-0.48-0.41 h-3.84c-0.24,0-0.43,0.17-0.47,0.41L9.25,5.35C8.66,5.59,8.12,5.92,7.63,6.29L5.24,5.33c-0.22-0.08-0.47,0-0.59,0.22L2.74,8.87 C2.62,9.08,2.66,9.34,2.86,9.48l2.03,1.58C4.84,11.36,4.8,11.69,4.8,12s0.02,0.64,0.07,0.94l-2.03,1.58 c-0.18,0.14-0.23,0.41-0.12,0.61l1.92,3.32c0.12,0.22,0.37,0.29,0.59,0.22l2.39-0.96c0.5,0.38,1.03,0.7,1.62,0.94l0.36,2.54 c0.05,0.24,0.24,0.41,0.48,0.41h3.84c0.24,0,0.44-0.17,0.47-0.41l0.36-2.54c0.59-0.24,1.13-0.56,1.62-0.94l2.39,0.96 c0.22,0.08,0.47,0,0.59-0.22l1.92-3.32c0.12-0.22,0.07-0.47-0.12-0.61L19.14,12.94z M12,15.6c-1.98,0-3.6-1.62-3.6-3.6 s1.62-3.6,3.6-3.6s3.6,1.62,3.6,3.6S13.98,15.6,12,15.6z"/></svg></div>
        <div id="modal-overlay" onclick="toggleSettings(false)"><div class="modal" onclick="event.stopPropagation()">
          <div class="modal-title">Settings</div><div class="modal-scroll">
          <div class="input-group"><label>RA Username</label><input type="text" id="input-user"></div>
          <div class="input-group"><label>API Key</label><input type="password" id="input-key"></div>
          <div class="modal-divider"></div><div id="filter-list"></div></div>
          <button class="btn-save" onclick="saveCredentials()">Save & Refresh</button>
        </div></div>
        <div class="dashboard">
          <div id="session-timer" class="session-timer">00:00</div>
          <p class="game-title" id="game-title">Waiting...</p>
          <div class="telemetry-grid"><div class="telemetry-row">
            <div class="column"><span class="val-text" id="frametime">--ms</span><span class="val-text" id='fps'>--</span></div>
            <div class="column"><span class="val-text" id="cpu_util">--%</span><span class="val-text" id="temp_cpu">--°</span></div>
            <div class="column"><span class="val-text" id="gpu_util">--%</span><span class="val-text" id="temp_gpu">--°</span></div>
            <div class="column"><span class="val-text" id="battery">--%</span><span class="val-text" id="power_w">--W</span></div>
          </div><div class="telemetry-row"><span class="label">FRAME</span><span class="label">CPU</span><span class="label">GPU</span><span class="label">BATT</span></div></div>
          <div class="anchored-row"><div class="game-progress" id="progress-text">-- / --</div><div class="clock-container"><span class="clock" id="clock">--:--</span></div></div>
          <div class="progress-bar-bg"><div class="progress-bar-fill" id="progress-fill"></div></div>
        </div>
        <div class="wrapper">
            <div id="pinned-achievements"></div>
            <div class="content">
                <div id="achievement-list"></div>
            </div>
        </div>
        </body></html>"""
    }
}
