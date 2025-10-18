package com.example.hearing_test
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.sin

class MainActivity : AppCompatActivity() {

    // 테스트할 주파수 목록 (Hz 단위)
    private val testFrequencies = intArrayOf(250, 500, 1000, 2000, 4000, 6000, 8000)
    private var currentFrequencyIndex = 0
    private var currentAmplitude = 1000 // 소리의 세기 (시작 값)
    private val maxAmplitude = 32767 // 최대 소리 세기 (Short.MAX_VALUE)
    private val amplitudeStep = 2000 // 소리 키우는 단계

    private var audioTrack: AudioTrack? = null
    private var isTesting = false
    private val toneDuration = 1.0 // 소리 재생 시간 (1초)

    // 각 주파수별 청력 임계값(들리기 시작한 최소 세기)을 저장할 맵
    private val hearingThresholds = mutableMapOf<Int, Int>()

    // UI 요소
    private lateinit var statusTextView: TextView
    private lateinit var startButton: Button
    private lateinit var heardButton: Button
    private lateinit var resultTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // XML 레이아웃 대신 코드로 UI를 직접 생성합니다.
        setupUI()
    }

    private fun setupUI() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
        }

        statusTextView = TextView(this).apply {
            text = "청력 테스트를 시작하려면 아래 버튼을 누르세요."
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 50)
        }

        startButton = Button(this).apply {
            text = "테스트 시작"
            setOnClickListener {
                if (!isTesting) {
                    startTest()
                }
            }
        }

        heardButton = Button(this).apply {
            text = "소리가 들려요"
            isEnabled = false // 테스트 시작 전에는 비활성화
            setOnClickListener {
                if (isTesting) {
                    soundHeard()
                }
            }
        }

        resultTextView = TextView(this).apply {
            text = ""
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 50, 0, 0)
        }

        layout.addView(statusTextView)
        layout.addView(startButton)
        layout.addView(heardButton)
        layout.addView(resultTextView)

        setContentView(layout)
    }

    private fun startTest() {
        // 테스트 변수 초기화
        currentFrequencyIndex = 0
        hearingThresholds.clear()
        resultTextView.text = ""
        isTesting = true
        startButton.isEnabled = false
        heardButton.isEnabled = true

        // 첫 번째 주파수 테스트 시작
        testNextFrequency()
    }

    private fun testNextFrequency() {
        if (currentFrequencyIndex < testFrequencies.size) {
            currentAmplitude = 1000 // 각 주파수마다 소리 세기 초기화
            val frequency = testFrequencies[currentFrequencyIndex]
            statusTextView.text = "${frequency}Hz 주파수를 테스트합니다.\n소리가 들리면 버튼을 눌러주세요."
            playToneWithIncreasingAmplitude()
        } else {
            // 모든 테스트 완료
            finishTest()
        }
    }

    private fun playToneWithIncreasingAmplitude() {
        if (!isTesting) return

        // 현재 소리 세기가 최대치를 넘으면, 들리지 않은 것으로 간주하고 다음 주파수로 이동
        if (currentAmplitude > maxAmplitude) {
            hearingThresholds[testFrequencies[currentFrequencyIndex]] = -1 // -1은 들리지 않음을 의미
            currentFrequencyIndex++
            testNextFrequency()
            return
        }

        playTone(testFrequencies[currentFrequencyIndex].toDouble(), currentAmplitude)

        // 1초 후에 소리를 멈추고, 잠시 후 다음 세기로 다시 재생
        Handler(Looper.getMainLooper()).postDelayed({
            stopTone()
            currentAmplitude += amplitudeStep // 소리 세기 증가
            if (isTesting) { // "들려요" 버튼이 눌리지 않았다면 계속 진행
                Handler(Looper.getMainLooper()).postDelayed({
                    playToneWithIncreasingAmplitude()
                }, 500) // 0.5초 대기
            }
        }, (toneDuration * 1000).toLong())
    }


    private fun soundHeard() {
        val frequency = testFrequencies[currentFrequencyIndex]
        hearingThresholds[frequency] = currentAmplitude
        isTesting = false // 현재 주파수의 자동 증가를 멈춤
        stopTone()

        // 다음 주파수 테스트 준비
        currentFrequencyIndex++
        Handler(Looper.getMainLooper()).postDelayed({
            isTesting = true
            testNextFrequency()
        }, 1000) // 1초 후 다음 테스트 시작
    }

    private fun finishTest() {
        isTesting = false
        startButton.isEnabled = true
        startButton.text = "다시 테스트"
        heardButton.isEnabled = false

        statusTextView.text = "테스트가 완료되었습니다."

        // 결과 분석 및 표시
        var resultText = "테스트 결과:\n"
        var bestFrequency = -1
        var lowestThreshold = Int.MAX_VALUE

        for ((freq, threshold) in hearingThresholds) {
            if (threshold != -1) {
                resultText += "${freq}Hz: 세기 ${threshold}에서 감지\n"
                if (threshold < lowestThreshold) {
                    lowestThreshold = threshold
                    bestFrequency = freq
                }
            } else {
                resultText += "${freq}Hz: 감지되지 않음\n"
            }
        }

        if (bestFrequency != -1) {
            resultText += "\n가장 민감하게 들리는 주파수는 ${bestFrequency}Hz 입니다."
        } else {
            resultText += "\n감지된 주파수가 없습니다."
        }

        resultTextView.text = resultText
    }

    private fun playTone(freqOfTone: Double, amplitude: Int) {
        stopTone() // 이전 소리가 있다면 정지

        val sampleRate = 44100
        val numSamples = (toneDuration * sampleRate).toInt()
        val generatedSnd = ByteArray(2 * numSamples)
        val sample = DoubleArray(numSamples)

        for (i in 0 until numSamples) {
            sample[i] = sin(2.0 * Math.PI * i.toDouble() / (sampleRate / freqOfTone))
        }

        var idx = 0
        for (dVal in sample) {
            val shortVal = (dVal * amplitude).toInt().toShort()
            generatedSnd[idx++] = (shortVal.toInt() and 0x00ff).toByte()
            generatedSnd[idx++] = ((shortVal.toInt() and 0xff00) ushr 8).toByte()
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
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(generatedSnd.size)
                .build()

            audioTrack?.write(generatedSnd, 0, generatedSnd.size)
            audioTrack?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopTone() {
        audioTrack?.let {
            if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                it.stop()
            }
            it.release()
        }
        audioTrack = null
    }

    override fun onStop() {
        super.onStop()
        stopTone() // 앱이 백그라운드로 가면 소리 정지
        isTesting = false
    }
}
