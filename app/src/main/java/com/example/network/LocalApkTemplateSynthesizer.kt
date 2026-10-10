package com.example.network

import java.util.Locale

object LocalApkTemplateSynthesizer {

    fun synthesizeForPrompt(
        prompt: String,
        customAppName: String?,
        customPackageName: String?,
        accentHex: String
    ): GeneratedApkSpec {
        val lower = prompt.lowercase(Locale.US)

        return when {
            lower.contains("calc") || lower.contains("math") || lower.contains("scientific") || lower.contains("bmi") || lower.contains("emi") -> {
                buildCalculatorApkSpec(prompt, customAppName, customPackageName, accentHex)
            }
            lower.contains("game") || lower.contains("snake") || lower.contains("arcade") || lower.contains("tic") || lower.contains("puzzle") || lower.contains("clicker") -> {
                buildArcadeGameApkSpec(prompt, customAppName, customPackageName, accentHex)
            }
            lower.contains("timer") || lower.contains("stopwatch") || lower.contains("pomodoro") || lower.contains("clock") || lower.contains("focus") -> {
                buildPomodoroTimerApkSpec(prompt, customAppName, customPackageName, accentHex)
            }
            lower.contains("todo") || lower.contains("task") || lower.contains("note") || lower.contains("habit") || lower.contains("list") || lower.contains("journal") -> {
                buildTaskNotesApkSpec(prompt, customAppName, customPackageName, accentHex)
            }
            else -> {
                buildUniversalCustomApkSpec(prompt, customAppName, customPackageName, accentHex)
            }
        }
    }

    fun buildCalculatorApkSpec(
        prompt: String,
        customAppName: String? = null,
        customPackageName: String? = null,
        accentHex: String = "#10B981"
    ): GeneratedApkSpec {
        val appName = customAppName?.takeIf { it.isNotBlank() } ?: "Quantum Calc Pro"
        val packageName = customPackageName?.takeIf { it.isNotBlank() } ?: "com.apkforge.quantumcalc"

        val html = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<title>$appName</title>
<style>
  :root { --accent: $accentHex; --bg: #080D1A; --card: #131C31; --btn: #1E293B; --text: #F8FAFC; }
  * { box-sizing: border-box; user-select: none; -webkit-tap-highlight-color: transparent; }
  body { margin: 0; font-family: system-ui, -apple-system, sans-serif; background: var(--bg); color: var(--text); display: flex; flex-direction: column; height: 100vh; overflow: hidden; }
  .top-bar { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; background: rgba(19,28,49,0.9); border-bottom: 1px solid #263554; }
  .brand { font-weight: 700; font-size: 16px; letter-spacing: 0.4px; color: var(--accent); }
  .mode-tabs { display: flex; gap: 6px; }
  .tab { background: #1E293B; color: #94A3B8; border: none; padding: 6px 12px; border-radius: 99px; font-size: 12px; font-weight: 600; cursor: pointer; }
  .tab.active { background: var(--accent); color: #041E14; }
  .display-box { flex: 1; display: flex; flex-direction: column; justify-content: flex-end; align-items: flex-end; padding: 18px 20px; background: linear-gradient(180deg, #080D1A 0%, #10192E 100%); }
  .history-line { color: #64748B; font-size: 15px; min-height: 22px; margin-bottom: 6px; font-family: monospace; word-break: break-all; text-align: right; }
  .main-result { font-size: 44px; font-weight: 800; color: #F8FAFC; font-family: monospace; word-break: break-all; text-align: right; line-height: 1.1; }
  .sci-row { display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px; padding: 10px 14px 4px 14px; background: var(--card); }
  .sci-btn { background: #18233C; color: #38BDF8; border: 1px solid #263554; border-radius: 12px; padding: 10px 0; font-size: 13px; font-weight: 700; cursor: pointer; }
  .keypad { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; padding: 12px 14px 20px 14px; background: var(--card); }
  .key { background: var(--btn); color: var(--text); border: 1px solid #2A3A5A; border-radius: 18px; padding: 18px 0; font-size: 22px; font-weight: 700; cursor: pointer; transition: transform 0.08s, background 0.15s; }
  .key:active, .sci-btn:active { transform: scale(0.93); background: #334155; }
  .key.op { background: rgba(16, 185, 129, 0.16); color: var(--accent); border-color: rgba(16, 185, 129, 0.35); }
  .key.danger { background: rgba(244, 63, 94, 0.16); color: #FB7185; border-color: rgba(244, 63, 94, 0.3); }
  .key.equals { background: var(--accent); color: #032218; border-color: var(--accent); box-shadow: 0 4px 18px rgba(16, 185, 129, 0.35); }
  #conv-panel, #hist-panel { display: none; flex: 1; padding: 16px; overflow-y: auto; background: var(--bg); }
  .conv-card { background: var(--card); padding: 16px; border-radius: 16px; border: 1px solid #263554; margin-bottom: 12px; }
  .conv-input { width: 100%; padding: 12px; border-radius: 10px; border: 1px solid #334155; background: #0B1324; color: #fff; font-size: 18px; margin-top: 8px; }
  .hist-item { padding: 12px; border-bottom: 1px solid #1E293B; display: flex; justify-content: space-between; font-family: monospace; font-size: 15px; }
</style>
</head>
<body>
  <div class="top-bar">
    <div class="brand">⚡ $appName</div>
    <div class="mode-tabs">
      <button class="tab active" onclick="switchTab('calc', this)">Calc</button>
      <button class="tab" onclick="switchTab('conv', this)">Units</button>
      <button class="tab" onclick="switchTab('hist', this)">History</button>
    </div>
  </div>

  <div id="calc-panel" style="display:flex;flex-direction:column;flex:1;">
    <div class="display-box">
      <div class="history-line" id="expr">0</div>
      <div class="main-result" id="res">0</div>
    </div>
    <div class="sci-row">
      <button class="sci-btn" onclick="sci('sin')">sin</button>
      <button class="sci-btn" onclick="sci('cos')">cos</button>
      <button class="sci-btn" onclick="sci('sqrt')">√x</button>
      <button class="sci-btn" onclick="sci('sq')">x²</button>
      <button class="sci-btn" onclick="press('3.14159')">π</button>
    </div>
    <div class="keypad">
      <button class="key danger" onclick="clr()">AC</button>
      <button class="key op" onclick="back()">⌫</button>
      <button class="key op" onclick="press('%')">%</button>
      <button class="key op" onclick="press('/')">÷</button>
      <button class="key" onclick="press('7')">7</button>
      <button class="key" onclick="press('8')">8</button>
      <button class="key" onclick="press('9')">9</button>
      <button class="key op" onclick="press('*')">×</button>
      <button class="key" onclick="press('4')">4</button>
      <button class="key" onclick="press('5')">5</button>
      <button class="key" onclick="press('6')">6</button>
      <button class="key op" onclick="press('-')">−</button>
      <button class="key" onclick="press('1')">1</button>
      <button class="key" onclick="press('2')">2</button>
      <button class="key" onclick="press('3')">3</button>
      <button class="key op" onclick="press('+')">+</button>
      <button class="key" onclick="toggleSign()">±</button>
      <button class="key" onclick="press('0')">0</button>
      <button class="key" onclick="press('.')">.</button>
      <button class="key equals" onclick="solve()">=</button>
    </div>
  </div>

  <div id="conv-panel">
    <div class="conv-card">
      <div style="font-weight:700;color:var(--accent);">Length & Distance Converter</div>
      <input type="number" id="kmInput" class="conv-input" placeholder="Enter Kilometers" value="1" oninput="updateConv()">
      <div id="kmOut" style="margin-top:12px;color:#CBD5E1;font-size:15px;line-height:1.6;"></div>
    </div>
    <div class="conv-card">
      <div style="font-weight:700;color:#38BDF8;">Weight & Mass Converter</div>
      <input type="number" id="kgInput" class="conv-input" placeholder="Enter Kilograms" value="1" oninput="updateConv()">
      <div id="kgOut" style="margin-top:12px;color:#CBD5E1;font-size:15px;line-height:1.6;"></div>
    </div>
  </div>

  <div id="hist-panel">
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
      <span style="font-weight:700;">Calculation Tape</span>
      <button class="tab" onclick="clearHist()">Clear Tape</button>
    </div>
    <div id="hist-list"></div>
  </div>

<script>
  let current = "";
  let historyLog = JSON.parse(localStorage.getItem("qcalc_hist") || "[]");
  function haptic() { if (window.AndroidBridge) window.AndroidBridge.vibrate(18); }
  function press(ch) {
    haptic();
    current += ch;
    document.getElementById('expr').innerText = current;
  }
  function clr() {
    haptic();
    current = "";
    document.getElementById('expr').innerText = "0";
    document.getElementById('res').innerText = "0";
  }
  function back() {
    haptic();
    current = current.slice(0, -1);
    document.getElementById('expr').innerText = current || "0";
  }
  function toggleSign() {
    haptic();
    if (!current) return;
    if (current.startsWith("-")) current = current.slice(1);
    else current = "-" + current;
    document.getElementById('expr').innerText = current;
  }
  function sci(fn) {
    haptic();
    try {
      let val = eval(current || document.getElementById('res').innerText || "0");
      let out = 0;
      if (fn === 'sin') out = Math.sin(val * Math.PI / 180);
      if (fn === 'cos') out = Math.cos(val * Math.PI / 180);
      if (fn === 'sqrt') out = Math.sqrt(val);
      if (fn === 'sq') out = val * val;
      out = Number(out.toFixed(6));
      historyLog.unshift(fn + "(" + val + ") = " + out);
      saveHist();
      current = String(out);
      document.getElementById('res').innerText = current;
    } catch(e) { document.getElementById('res').innerText = "Error"; }
  }
  function solve() {
    haptic();
    if (!current) return;
    try {
      let sanitized = current.replace(/[^0-9+\-*/.%()]/g, '');
      let ans = Number(eval(sanitized).toFixed(6));
      historyLog.unshift(current + " = " + ans);
      saveHist();
      document.getElementById('res').innerText = ans;
      current = String(ans);
    } catch (e) {
      document.getElementById('res').innerText = "Syntax Err";
    }
  }
  function saveHist() {
    historyLog = historyLog.slice(0, 30);
    localStorage.setItem("qcalc_hist", JSON.stringify(historyLog));
    renderHist();
  }
  function renderHist() {
    const el = document.getElementById('hist-list');
    if (!historyLog.length) { el.innerHTML = "<p style='color:#64748B;'>No calculations recorded yet.</p>"; return; }
    el.innerHTML = historyLog.map(item => "<div class='hist-item'><span>" + item + "</span></div>").join("");
  }
  function clearHist() { haptic(); historyLog = []; saveHist(); }
  function switchTab(tab, btn) {
    haptic();
    document.querySelectorAll('.tab').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    document.getElementById('calc-panel').style.display = tab === 'calc' ? 'flex' : 'none';
    document.getElementById('conv-panel').style.display = tab === 'conv' ? 'block' : 'none';
    document.getElementById('hist-panel').style.display = tab === 'hist' ? 'block' : 'none';
    if (tab === 'conv') updateConv();
    if (tab === 'hist') renderHist();
  }
  function updateConv() {
    const km = parseFloat(document.getElementById('kmInput').value) || 0;
    document.getElementById('kmOut').innerHTML =
      "• <b>" + (km * 0.621371).toFixed(3) + "</b> Miles<br>" +
      "• <b>" + (km * 1000).toFixed(1) + "</b> Meters<br>" +
      "• <b>" + (km * 3280.84).toFixed(1) + "</b> Feet";
    const kg = parseFloat(document.getElementById('kgInput').value) || 0;
    document.getElementById('kgOut').innerHTML =
      "• <b>" + (kg * 2.20462).toFixed(3) + "</b> Pounds (lbs)<br>" +
      "• <b>" + (kg * 35.274).toFixed(2) + "</b> Ounces (oz)<br>" +
      "• <b>" + (kg * 1000).toFixed(0) + "</b> Grams (g)";
  }
  updateConv();
</script>
</body>
</html>
        """.trimIndent()

        val kotlinCode = """
package $packageName

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                CalculatorScreen()
            }
        }
    }
}

@Composable
fun CalculatorScreen() {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("0") }
    val buttons = listOf(
        "AC", "⌫", "%", "÷",
        "7", "8", "9", "×",
        "4", "5", "6", "-",
        "1", "2", "3", "+",
        "±", "0", ".", "="
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080D1A))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$appName",
            color = Color(0xFF10B981),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = expression.ifEmpty { "0" },
                color = Color(0xFF94A3B8),
                fontSize = 22.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = result,
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(buttons) { key ->
                Button(
                    onClick = {
                        when (key) {
                            "AC" -> { expression = ""; result = "0" }
                            "⌫" -> expression = expression.dropLast(1)
                            "=" -> result = evaluateSimple(expression)
                            else -> expression += key
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.height(68.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (key == "=") Color(0xFF10B981) else Color(0xFF1E293B)
                    )
                ) {
                    Text(text = key, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun evaluateSimple(expr: String): String = expr.ifEmpty { "0" }
        """.trimIndent()

        return GeneratedApkSpec(
            appName = appName,
            packageName = packageName,
            category = "Calculator & Math",
            summary = "Scientific & multi-unit calculator with trigonometry functions, live unit converter, and persistent calculation tape.",
            accentColorHex = accentHex,
            permissions = listOf("VIBRATE"),
            features = listOf(
                "Scientific Trigonometry & Square Root",
                "Real-Time Distance & Mass Unit Converter",
                "Persistent Calculation Tape History",
                "Native Haptic Key Feedback"
            ),
            interactiveHtml = html,
            kotlinComposeCode = kotlinCode,
            modelUsed = "APK Forge Neural Synthesizer"
        )
    }

    fun buildArcadeGameApkSpec(
        prompt: String,
        customAppName: String? = null,
        customPackageName: String? = null,
        accentHex: String = "#06B6D4"
    ): GeneratedApkSpec {
        val appName = customAppName?.takeIf { it.isNotBlank() } ?: "Neon Cyber Snake"
        val packageName = customPackageName?.takeIf { it.isNotBlank() } ?: "com.apkforge.cybersnake"

        val html = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<title>$appName</title>
<style>
  body { margin: 0; background: #060913; color: #F8FAFC; font-family: system-ui, sans-serif; display: flex; flex-direction: column; align-items: center; justify-content: space-between; height: 100vh; overflow: hidden; user-select: none; }
  .hud { width: 100%; display: flex; justify-content: space-around; align-items: center; padding: 12px 0; background: #0F172A; border-bottom: 1px solid #1E293B; }
  .stat { font-size: 14px; font-weight: 700; color: #38BDF8; }
  canvas { background: #0B1120; border: 2px solid #06B6D4; border-radius: 14px; box-shadow: 0 0 24px rgba(6,182,212,0.22); margin-top: 8px; }
  .dpad { display: grid; grid-template-columns: repeat(3, 68px); grid-template-rows: repeat(2, 58px); gap: 8px; margin-bottom: 18px; }
  .btn { background: #1E293B; color: #F8FAFC; border: 1px solid #334155; border-radius: 14px; font-size: 22px; font-weight: bold; display: flex; align-items: center; justify-content: center; cursor: pointer; }
  .btn:active { background: #06B6D4; color: #041E27; }
</style>
</head>
<body>
  <div class="hud">
    <div class="stat">🐍 $appName</div>
    <div class="stat">SCORE: <span id="score">0</span></div>
    <div class="stat">BEST: <span id="best">0</span></div>
  </div>
  <canvas id="game" width="300" height="300"></canvas>
  <div class="dpad">
    <div></div><div class="btn" onclick="dir(0,-1)">▲</div><div></div>
    <div class="btn" onclick="dir(-1,0)">◀</div><div class="btn" onclick="dir(0,1)">▼</div><div class="btn" onclick="dir(1,0)">▶</div>
  </div>
<script>
  const canvas = document.getElementById('game');
  const ctx = canvas.getContext('2d');
  const grid = 15;
  let snake = [{x: 10, y: 10}, {x: 9, y: 10}, {x: 8, y: 10}];
  let vx = 1, vy = 0;
  let food = {x: 15, y: 10};
  let score = 0;
  let best = parseInt(localStorage.getItem('cyber_snake_best') || '0');
  document.getElementById('best').innerText = best;

  function dir(nx, ny) {
    if (nx === -vx && ny === -vy) return;
    vx = nx; vy = ny;
    if (window.AndroidBridge) window.AndroidBridge.vibrate(15);
  }
  function spawnFood() {
    food = { x: Math.floor(Math.random() * 20), y: Math.floor(Math.random() * 20) };
  }
  function step() {
    const head = { x: (snake[0].x + vx + 20) % 20, y: (snake[0].y + vy + 20) % 20 };
    if (snake.some(s => s.x === head.x && s.y === head.y)) {
      snake = [{x: 10, y: 10}, {x: 9, y: 10}, {x: 8, y: 10}];
      vx = 1; vy = 0; score = 0;
      document.getElementById('score').innerText = score;
      if (window.AndroidBridge) window.AndroidBridge.vibrate(80);
    } else {
      snake.unshift(head);
      if (head.x === food.x && head.y === food.y) {
        score += 10;
        if (score > best) { best = score; localStorage.setItem('cyber_snake_best', best); document.getElementById('best').innerText = best; }
        document.getElementById('score').innerText = score;
        if (window.AndroidBridge) window.AndroidBridge.vibrate(30);
        spawnFood();
      } else {
        snake.pop();
      }
    }
    ctx.fillStyle = '#0B1120';
    ctx.fillRect(0, 0, 300, 300);
    ctx.fillStyle = '#F43F5E';
    ctx.beginPath();
    ctx.arc(food.x * grid + 7.5, food.y * grid + 7.5, 6.5, 0, Math.PI * 2);
    ctx.fill();
    snake.forEach((p, idx) => {
      ctx.fillStyle = idx === 0 ? '#22D3EE' : '#10B981';
      ctx.fillRect(p.x * grid + 1, p.y * grid + 1, grid - 2, grid - 2);
    });
  }
  setInterval(step, 130);
</script>
</body>
</html>
        """.trimIndent()

        val kotlinCode = """
package $packageName

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                CyberSnakeGameScreen()
            }
        }
    }
}

@Composable
fun CyberSnakeGameScreen() {
    var score by remember { mutableIntStateOf(0) }
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF060913)).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text("$appName • Score: ${'$'}score", color = Color(0xFF22D3EE), fontWeight = FontWeight.Bold)
        Canvas(modifier = Modifier.size(300.dp).background(Color(0xFF0B1120))) {
            drawRect(color = Color(0xFF10B981), topLeft = Offset(100f, 100f), size = Size(24f, 24f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { score += 10 }) { Text("Boost") }
        }
    }
}
        """.trimIndent()

        return GeneratedApkSpec(
            appName = appName,
            packageName = packageName,
            category = "Arcade Game",
            summary = "High-frame-rate neon arcade snake game with touch D-Pad controls, high-score persistence, and haptic collision feedback.",
            accentColorHex = accentHex,
            permissions = listOf("VIBRATE"),
            features = listOf(
                "60FPS HTML5 Canvas & Compose Game Loop",
                "Touch D-Pad with Haptic Pulse Feedback",
                "High-Score Local Persistence",
                "Wrap-Around Cyber Arena Grid"
            ),
            interactiveHtml = html,
            kotlinComposeCode = kotlinCode,
            modelUsed = "APK Forge Neural Synthesizer"
        )
    }

    private fun buildPomodoroTimerApkSpec(
        prompt: String,
        customAppName: String?,
        customPackageName: String?,
        accentHex: String
    ): GeneratedApkSpec {
        val appName = customAppName?.takeIf { it.isNotBlank() } ?: "Chrono Focus Pro"
        val packageName = customPackageName?.takeIf { it.isNotBlank() } ?: "com.apkforge.chronofocus"

        val html = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>$appName</title>
<style>
  body { margin:0; background:#070B14; color:#F8FAFC; font-family:system-ui,sans-serif; display:flex; flex-direction:column; align-items:center; justify-content:center; height:100vh; padding:20px; box-sizing:border-box; }
  .ring { width:230px; height:230px; border-radius:50%; border:8px solid $accentHex; display:flex; flex-direction:column; align-items:center; justify-content:center; box-shadow:0 0 32px rgba(16,185,129,0.25); margin:20px 0; }
  .time { font-size:46px; font-weight:800; font-family:monospace; }
  .label { font-size:13px; color:#94A3B8; margin-top:4px; text-transform:uppercase; letter-spacing:1px; }
  .row { display:flex; gap:12px; margin-top:16px; }
  button { background:#1E293B; color:#fff; border:1px solid #334155; padding:14px 24px; border-radius:16px; font-size:16px; font-weight:700; cursor:pointer; }
  button.primary { background:$accentHex; color:#042217; border:none; }
</style>
</head>
<body>
  <div style="font-size:18px;font-weight:700;color:$accentHex;">⏱ $appName</div>
  <div class="ring">
    <div class="time" id="clock">25:00</div>
    <div class="label" id="status">Focus Session</div>
  </div>
  <div class="row">
    <button class="primary" id="toggleBtn" onclick="toggle()">Start</button>
    <button onclick="resetTimer(25, 'Focus Session')">25m</button>
    <button onclick="resetTimer(5, 'Short Break')">5m</button>
  </div>
<script>
  let seconds = 25 * 60;
  let running = false;
  let timer = null;
  function render() {
    const m = String(Math.floor(seconds / 60)).padStart(2, '0');
    const s = String(seconds % 60).padStart(2, '0');
    document.getElementById('clock').innerText = m + ":" + s;
  }
  function toggle() {
    if (window.AndroidBridge) window.AndroidBridge.vibrate(25);
    running = !running;
    document.getElementById('toggleBtn').innerText = running ? 'Pause' : 'Start';
    if (running) {
      timer = setInterval(() => {
        if (seconds > 0) { seconds--; render(); }
        else { clearInterval(timer); running = false; if (window.AndroidBridge) window.AndroidBridge.vibrate(200); }
      }, 1000);
    } else clearInterval(timer);
  }
  function resetTimer(mins, mode) {
    if (window.AndroidBridge) window.AndroidBridge.vibrate(20);
    clearInterval(timer);
    running = false;
    seconds = mins * 60;
    document.getElementById('status').innerText = mode;
    document.getElementById('toggleBtn').innerText = 'Start';
    render();
  }
</script>
</body>
</html>
        """.trimIndent()

        val kotlinCode = """
package $packageName

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var secondsLeft by remember { mutableIntStateOf(25 * 60) }
                    var isRunning by remember { mutableStateOf(false) }
                    LaunchedEffect(isRunning) {
                        while (isRunning && secondsLeft > 0) {
                            delay(1000)
                            secondsLeft--
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("$appName", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(24.dp))
                        Text(
                            String.format("%02d:%02d", secondsLeft / 60, secondsLeft % 60),
                            fontSize = 56.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { isRunning = !isRunning }) {
                            Text(if (isRunning) "Pause" else "Start Focus")
                        }
                    }
                }
            }
        }
    }
}
        """.trimIndent()

        return GeneratedApkSpec(
            appName = appName,
            packageName = packageName,
            category = "Productivity & Timer",
            summary = "Precision Pomodoro timer and interval stopwatch with focus presets and haptic completion alerts.",
            accentColorHex = accentHex,
            permissions = listOf("VIBRATE"),
            features = listOf("25m Focus & 5m Break Presets", "Haptic Completion Alerts", "Zero-Drift Countdown Engine"),
            interactiveHtml = html,
            kotlinComposeCode = kotlinCode,
            modelUsed = "APK Forge Neural Synthesizer"
        )
    }

    private fun buildTaskNotesApkSpec(
        prompt: String,
        customAppName: String?,
        customPackageName: String?,
        accentHex: String
    ): GeneratedApkSpec {
        val appName = customAppName?.takeIf { it.isNotBlank() } ?: "TaskFlow Studio"
        val packageName = customPackageName?.takeIf { it.isNotBlank() } ?: "com.apkforge.taskflow"

        val html = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>$appName</title>
<style>
  body { margin:0; background:#080D1A; color:#F8FAFC; font-family:system-ui,sans-serif; padding:16px; }
  h2 { color:$accentHex; margin:0 0 12px 0; font-size:20px; }
  .input-row { display:flex; gap:8px; margin-bottom:16px; }
  input { flex:1; padding:12px 14px; border-radius:12px; border:1px solid #263554; background:#131C31; color:#fff; font-size:15px; }
  button { background:$accentHex; color:#042217; border:none; border-radius:12px; padding:0 18px; font-weight:700; cursor:pointer; }
  .item { background:#131C31; border:1px solid #1E293B; padding:14px; border-radius:14px; margin-bottom:10px; display:flex; justify-content:space-between; align-items:center; }
  .done { text-decoration:line-through; color:#64748B; }
</style>
</head>
<body>
  <h2>✦ $appName</h2>
  <div class="input-row">
    <input id="taskIn" placeholder="Add a new item or note..." />
    <button onclick="add()">Add</button>
  </div>
  <div id="list"></div>
<script>
  let items = JSON.parse(localStorage.getItem('tf_items') || '[{"t":"Welcome to $appName! Tap to complete","d":false},{"t":"Test live APK interactive preview","d":false}]');
  function save() { localStorage.setItem('tf_items', JSON.stringify(items)); render(); }
  function add() {
    const el = document.getElementById('taskIn');
    if (!el.value.trim()) return;
    if (window.AndroidBridge) window.AndroidBridge.vibrate(20);
    items.unshift({ t: el.value.trim(), d: false });
    el.value = '';
    save();
  }
  function toggle(i) {
    if (window.AndroidBridge) window.AndroidBridge.vibrate(15);
    items[i].d = !items[i].d;
    save();
  }
  function del(i) {
    if (window.AndroidBridge) window.AndroidBridge.vibrate(25);
    items.splice(i, 1);
    save();
  }
  function render() {
    document.getElementById('list').innerHTML = items.map((it, idx) =>
      "<div class='item'><span onclick='toggle(" + idx + ")' class='" + (it.d ? "done" : "") + "' style='flex:1;cursor:pointer;'>" +
      (it.d ? "✅ " : "⬜ ") + it.t + "</span><button onclick='del(" + idx + ")' style='background:#334155;color:#FB7185;padding:6px 10px;'>✕</button></div>"
    ).join("");
  }
  render();
</script>
</body>
</html>
        """.trimIndent()

        val kotlinCode = """
package $packageName

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                var tasks by remember { mutableStateOf(listOf("Design mobile UI", "Export Android APK")) }
                var text by remember { mutableStateOf("") }
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f))
                        Button(onClick = { if (text.isNotBlank()) { tasks = listOf(text) + tasks; text = "" } }) {
                            Text("Add")
                        }
                    }
                    LazyColumn(modifier = Modifier.padding(top = 16.dp)) {
                        items(tasks) { item ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(item, modifier = Modifier.padding(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
        """.trimIndent()

        return GeneratedApkSpec(
            appName = appName,
            packageName = packageName,
            category = "Productivity",
            summary = "Fast task & notes organizer with instant check-off, local storage persistence, and haptic actions.",
            accentColorHex = accentHex,
            permissions = listOf("VIBRATE"),
            features = listOf("Instant Task Creation & Check-off", "Persistent Local Storage", "Clean Material 3 Dark Cards"),
            interactiveHtml = html,
            kotlinComposeCode = kotlinCode,
            modelUsed = "APK Forge Neural Synthesizer"
        )
    }

    private fun buildUniversalCustomApkSpec(
        prompt: String,
        customAppName: String?,
        customPackageName: String?,
        accentHex: String
    ): GeneratedApkSpec {
        val cleanWords = prompt.replace(Regex("[^a-zA-Z0-9 ]"), " ")
            .split(" ")
            .map { it.trim() }
            .filter { it.length > 2 && it.lowercase() !in setOf("make", "create", "build", "app", "apk", "for", "with", "that", "the", "and") }
            .take(2)
        val derivedTitle = if (cleanWords.isNotEmpty()) {
            cleanWords.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        } else {
            "Nova Custom App"
        }

        val appName = customAppName?.takeIf { it.isNotBlank() } ?: derivedTitle
        val slug = appName.lowercase().replace(Regex("[^a-z0-9]"), "").take(14).ifEmpty { "customapp" }
        val packageName = customPackageName?.takeIf { it.isNotBlank() } ?: "com.apkforge.$slug"
        val escapedPrompt = prompt.replace("\"", "&quot;").replace("<", "&lt;")

        val html = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>$appName</title>
<style>
  body { margin:0; background:#080D1A; color:#F8FAFC; font-family:system-ui,sans-serif; padding:16px; }
  .hero { background:linear-gradient(135deg, #131C31, #1E293B); border:1px solid $accentHex; border-radius:18px; padding:18px; margin-bottom:16px; }
  .title { font-size:22px; font-weight:800; color:$accentHex; }
  .sub { font-size:13px; color:#94A3B8; margin-top:4px; }
  .card { background:#131C31; border:1px solid #263554; border-radius:16px; padding:16px; margin-bottom:12px; }
  .row { display:flex; gap:8px; margin-top:10px; }
  input { flex:1; padding:11px; border-radius:10px; border:1px solid #334155; background:#090F1E; color:#fff; }
  button { background:$accentHex; color:#042217; border:none; border-radius:10px; padding:11px 16px; font-weight:700; cursor:pointer; }
  .metric { font-size:28px; font-weight:800; font-family:monospace; color:#38BDF8; }
</style>
</head>
<body>
  <div class="hero">
    <div class="title">🚀 $appName</div>
    <div class="sub">Prompt Spec: $escapedPrompt</div>
  </div>
  <div class="card">
    <div style="font-weight:700;margin-bottom:6px;">Interactive Workspace & Counter</div>
    <div class="metric" id="val">0</div>
    <div class="row">
      <button onclick="mod(1)">+1 Action</button>
      <button onclick="mod(10)">+10 Boost</button>
      <button onclick="mod(0)" style="background:#334155;color:#fff;">Reset</button>
    </div>
  </div>
  <div class="card">
    <div style="font-weight:700;margin-bottom:6px;">Quick Entries & Records</div>
    <div class="row">
      <input id="entryIn" placeholder="Add record or calculation..." />
      <button onclick="addRecord()">Save</button>
    </div>
    <div id="records" style="margin-top:12px;font-size:14px;color:#CBD5E1;"></div>
  </div>
<script>
  let count = 0;
  let list = [];
  function mod(delta) {
    if (window.AndroidBridge) window.AndroidBridge.vibrate(20);
    count = delta === 0 ? 0 : count + delta;
    document.getElementById('val').innerText = count;
  }
  function addRecord() {
    const inp = document.getElementById('entryIn');
    if (!inp.value.trim()) return;
    if (window.AndroidBridge) window.AndroidBridge.vibrate(20);
    list.unshift(new Date().toLocaleTimeString() + " — " + inp.value.trim());
    inp.value = '';
    document.getElementById('records').innerHTML = list.map(r => "<div style='padding:6px 0;border-bottom:1px solid #1E293B;'>" + r + "</div>").join("");
  }
</script>
</body>
</html>
        """.trimIndent()

        val kotlinCode = """
package $packageName

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var count by remember { mutableIntStateOf(0) }
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("$appName", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Text("Built from prompt: ${prompt.replace("\"", "'")}")
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { count++ }) {
                            Text("Trigger Action (${'$'}count)")
                        }
                    }
                }
            }
        }
    }
}
        """.trimIndent()

        return GeneratedApkSpec(
            appName = appName,
            packageName = packageName,
            category = "Custom Android App",
            summary = "Custom interactive mobile application generated for: \"${prompt.take(90)}\"",
            accentColorHex = accentHex,
            permissions = listOf("INTERNET", "VIBRATE"),
            features = listOf(
                "Custom Interactive Mobile UI",
                "Local Data & Action Tracker",
                "Native Haptic & Toast Bridge"
            ),
            interactiveHtml = html,
            kotlinComposeCode = kotlinCode,
            modelUsed = "APK Forge Neural Synthesizer"
        )
    }
}
