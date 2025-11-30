package com.example.hearing_test

import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private var audioTrack: AudioTrack? = null
    private val sampleRate = 44100

    // --- 공통 변수 ---
    private var isTesting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showMainMenu() // 앱 시작 시 메인 메뉴 표시
    }

    // ==========================================
    // 1. 메인 메뉴 화면 (기능 선택)
    // ==========================================
    private fun showMainMenu() {
        stopAudio() // 혹시 재생 중인 소리가 있다면 정지

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(50, 50, 50, 50)
            setBackgroundColor(Color.WHITE)
        }

        val titleView = TextView(this).apply {
            text = "청력 테스트 모음"
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 100)
            setTextColor(Color.BLACK)
        }

        val btnAgeTest = Button(this).apply {
            text = "👂 청력 나이 테스트\n(고주파수 8k~20k Hz)"
            textSize = 18f
            setPadding(0, 40, 0, 40)
            setOnClickListener { startHearingAgeTest() }
        }

        val btnStereoTest = Button(this).apply {
            text = "🎧 좌우 방향 테스트\n(스테레오 감각)"
            textSize = 18f
            setPadding(0, 40, 0, 40)
            setOnClickListener { startStereoTest() }
            // 버튼 간 간격 띄우기
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 50 }
        }

        layout.addView(titleView)
        layout.addView(btnAgeTest)
        layout.addView(btnStereoTest)

        setContentView(layout)
    }


    // ==========================================
    // 2. 청력 나이 테스트 (기존 기능)
    // ==========================================
    private fun startHearingAgeTest() {
        val frequencies = intArrayOf(8000, 10000, 12000, 14000, 15000, 16000, 17000, 18000, 19000, 20000)
        var currentIndex = 0
        val audibleList = mutableListOf<Int>()
        var amplitude = 5000
        val maxAmp = 32767

        // UI 생성
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
        }

        val title = TextView(this).apply {
            text = "[ 청력 나이 테스트 ]"
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }

        val statusText = TextView(this).apply {
            text = "준비됨"
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }

        val volText = TextView(this).apply { text = "소리 세기: -" }

        // 내부 함수: 다음 주파수 진행
        fun nextFreq() {
            if (currentIndex < frequencies.size) {
                amplitude = 5000
                val freq = frequencies[currentIndex]
                statusText.text = "현재 주파수: ${freq}Hz\n들리면 '들려요', 안 들리면 '안 들려요'를 누르세요."
                val percent = (amplitude.toFloat() / maxAmp * 100).toInt()
                volText.text = "소리 강도: $percent%"
            } else {
                // 결과 분석
                stopAudio()
                val maxFreq = audibleList.maxOrNull() ?: 0
                val ageResult = when {
                    maxFreq >= 19000 -> "10대 이하 (최상)"
                    maxFreq >= 17000 -> "20대 초반"
                    maxFreq >= 16000 -> "20대 후반"
                    maxFreq >= 15000 -> "30대"
                    maxFreq >= 14000 -> "40대"
                    maxFreq >= 12000 -> "50대"
                    maxFreq >= 10000 -> "60대"
                    maxFreq >= 8000 -> "60대 이상"
                    else -> "난청 의심 (8000Hz 미만)"
                }
                statusText.text = "테스트 완료!\n\n감지 최고 주파수: ${maxFreq}Hz\n당신의 청력 나이: $ageResult"
                volText.text = ""
            }
        }

        // 버튼들
        val btnPlay = Button(this).apply {
            text = "소리 재생"
            setOnClickListener {
                if(currentIndex < frequencies.size) {
                    playTone(frequencies[currentIndex].toDouble(), amplitude, 0.5, "BOTH")
                }
            }
        }

        val btnVolUp = Button(this).apply {
            text = "소리 키움 (+)"
            setOnClickListener {
                if (amplitude < maxAmp) amplitude += 2000
                val percent = (amplitude.toFloat() / maxAmp * 100).toInt()
                volText.text = "소리 강도: $percent%"
            }
        }

        val btnHeard = Button(this).apply {
            text = "들려요 (성공)"
            setOnClickListener {
                if(currentIndex < frequencies.size) {
                    stopAudio()
                    audibleList.add(frequencies[currentIndex])
                    currentIndex++
                    nextFreq()
                }
            }
        }

        val btnSkip = Button(this).apply {
            text = "안 들려요 (다음)"
            setOnClickListener {
                if(currentIndex < frequencies.size) {
                    stopAudio()
                    currentIndex++
                    nextFreq()
                }
            }
        }

        val btnHome = Button(this).apply {
            text = "메인 메뉴로 돌아가기"
            setOnClickListener { showMainMenu() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 50 }
        }

        layout.addView(title)
        layout.addView(statusText)
        layout.addView(volText)
        layout.addView(btnVolUp)
        layout.addView(btnPlay)
        layout.addView(btnSkip)
        layout.addView(btnHeard)
        layout.addView(btnHome)

        setContentView(layout)
        nextFreq()
    }


    // ==========================================
    // 3. 좌우 방향 테스트 (새로운 기능)
    // ==========================================
    private fun startStereoTest() {
        var step = 1 // 1:좌, 2:우, 3:랜덤
        var targetSide = ""
        var score = 0

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
        }

        val title = TextView(this).apply {
            text = "[ 좌우 방향 테스트 ]\n이어폰을 착용해주세요."
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }

        val statusText = TextView(this).apply {
            text = "준비됨"
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }

        val resultText = TextView(this).apply {
            text = ""
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.BLUE)
            setPadding(0, 0, 0, 30)
        }

        val btnPlay = Button(this) // 선언 먼저

        val btnLeft = Button(this).apply {
            text = "◀ 왼쪽"
            isEnabled = false
        }

        val btnRight = Button(this).apply {
            text = "오른쪽 ▶"
            isEnabled = false
        }

        val btnHome = Button(this).apply {
            text = "메인 메뉴로 돌아가기"
            setOnClickListener { showMainMenu() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 50 }
        }

        // 로직 함수들
        fun setupStep() {
            btnPlay.isEnabled = true
            btnPlay.text = "소리 재생 (단계 $step/3)"
            btnLeft.isEnabled = false
            btnRight.isEnabled = false

            when(step) {
                1 -> {
                    targetSide = "LEFT"
                    statusText.text = "1단계: 소리를 듣고 방향을 맞추세요."
                }
                2 -> {
                    targetSide = "RIGHT"
                    statusText.text = "2단계: 소리를 듣고 방향을 맞추세요."
                }
                3 -> {
                    targetSide = if(Random.nextBoolean()) "LEFT" else "RIGHT"
                    statusText.text = "3단계 (랜덤): 어디서 소리가 날까요?"
                }
                else -> {
                    statusText.text = "테스트 완료! 점수: $score / 3"
                    btnPlay.isEnabled = false
                    btnPlay.text = "완료"
                    val eval = if(score==3) "완벽합니다!" else "이어폰 좌우를 확인해보세요."
                    resultText.text = eval
                }
            }
        }

        fun check(ans: String) {
            if(ans == targetSide) {
                score++
                resultText.text = "정답!"
            } else {
                resultText.text = "틀렸습니다. (정답: $targetSide)"
            }
            step++
            setupStep()
        }

        btnPlay.apply {
            text = "테스트 시작"
            setOnClickListener {
                if(step <= 3) {
                    btnPlay.isEnabled = false
                    btnPlay.text = "재생 중..."
                    Thread {
                        playTone(500.0, 30000, 1.0, targetSide)
                        runOnUiThread {
                            btnPlay.text = "방향을 선택하세요"
                            btnLeft.isEnabled = true
                            btnRight.isEnabled = true
                        }
                    }.start()
                }
            }
        }

        btnLeft.setOnClickListener { check("LEFT") }
        btnRight.setOnClickListener { check("RIGHT") }

        layout.addView(title)
        layout.addView(statusText)
        layout.addView(btnPlay)

        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        buttonLayout.addView(btnLeft)
        buttonLayout.addView(btnRight)

        layout.addView(buttonLayout)
        layout.addView(resultText)
        layout.addView(btnHome)

        setContentView(layout)
        setupStep()
    }


    // ==========================================
    // 4. 오디오 재생 엔진 (공통 사용)
    // ==========================================
    private fun playTone(freq: Double, amplitude: Int, duration: Double, side: String) {
        stopAudio()

        val numSamples = (duration * sampleRate).toInt()
        val sample = DoubleArray(numSamples)
        // 16bit Stereo PCM: [L, R, L, R ...] (Byte 크기는 샘플 수 * 2채널 * 2바이트 = 4배)
        val generatedSnd = ByteArray(4 * numSamples)

        for (i in 0 until numSamples) {
            sample[i] = sin(2.0 * Math.PI * i.toDouble() / (sampleRate / freq))
        }

        var idx = 0
        for (dVal in sample) {
            val shortVal = (dVal * amplitude).toInt().toShort()
            val low = (shortVal.toInt() and 0x00ff).toByte()
            val high = ((shortVal.toInt() and 0xff00) ushr 8).toByte()

            // LEFT Channel
            if (side == "LEFT" || side == "BOTH") {
                generatedSnd[idx++] = low
                generatedSnd[idx++] = high
            } else {
                generatedSnd[idx++] = 0
                generatedSnd[idx++] = 0
            }

            // RIGHT Channel
            if (side == "RIGHT" || side == "BOTH") {
                generatedSnd[idx++] = low
                generatedSnd[idx++] = high
            } else {
                generatedSnd[idx++] = 0
                generatedSnd[idx++] = 0
            }
        }

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO) // 중요: 항상 스테레오로 설정
                        .build()
                )
                .setBufferSizeInBytes(generatedSnd.size)
                .build()

            audioTrack?.write(generatedSnd, 0, generatedSnd.size)
            audioTrack?.play()

            // UI 스레드를 멈추지 않기 위해 Thread.sleep은 백그라운드 스레드에서만 사용 권장
            // 여기서는 단순 호출용이라 놔두되, 실제 재생 길이는 데이터 양에 따라 결정됨

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAudio() {
        try {
            audioTrack?.let {
                if (it.state == AudioTrack.STATE_INITIALIZED) {
                    if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        it.stop()
                    }
                }
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioTrack = null
    }

    override fun onStop() {
        super.onStop()
        stopAudio()
    }
}