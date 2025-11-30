# 🎧 안드로이드 청력 & 방향성 진단 앱 (Android Hearing Test)

Native AudioTrack API를 활용하여 정밀한 주파수 제어와 스테레오 사운드를 구현한 청력 자가 진단 애플리케이션

---

## 💡 기획 의도 (Motivation)

현대인의 이어폰 사용 시간 증가로 인해 젊은 층에서도 **'소음성 난청'** 환자가 늘어나고 있습니다. 

특히 고주파수 대역의 청력 손실이나 좌우 청력의 불균형은 일상생활에서 쉽게 알아차리기 어렵습니다.

이 프로젝트는 *"병원에 가지 않고도, 누구나 가진 스마트폰으로 내 귀의 상태를 정밀하게 확인할 수 없을까?"* 라는 물음에서 시작되었습니다.

**자가 진단:** 8kHz~20kHz의 고주파 대역을 테스트하여 '청력 나이'를 산출합니다.

**기능성:** 단순 재생이 아닌, 좌/우 채널을 완벽하게 분리하여 한쪽 귀의 난청이나 이어폰의 밸런스 문제를 잡아냅니다.

**기술적 도전:** 무거운 외부 라이브러리 없이, 안드로이드 순정 API만으로 파동(Wave)을 직접 생성하여 앱의 용량을 줄이고 성능을 최적화했습니다.

**활용:** 본인의 주파수와 연령대를 찾고 사용하는 이어폰들의 주파수를 PC 또는 모바일에서 설정하여 최적의 소리를 들을 수 있도록 합니다.

## 📱 주요 기능 (Features)

### 1. 청력 나이 테스트 (Hearing Age Test)

* **원리:** 나이가 들수록 고음(높은 주파수)을 듣는 능력이 떨어진다는 점을 이용합니다.

* **범위:** 8,000Hz부터 인간의 가청 한계인 20,000Hz까지 단계별로 테스트합니다.

* **결과:** 사용자가 감지한 최대 주파수를 분석하여 '10대'부터 '60대 이상'까지 신체 청력 나이를 알려줍니다.

### 2. 좌우 방향성 테스트 (Stereo Direction Test)

* **원리:** 스테레오 채널 분리 기술을 사용하여 왼쪽과 오른쪽 이어폰에서 번갈아 소리를 냅니다.

* **로직:**

1. 왼쪽만 재생 (사용자 인지 확인)

2. 오른쪽만 재생 (사용자 인지 확인)

3. 랜덤 재생 (사용자의 실제 반응 속도 및 정확도 테스트)

* **효과:** 청각의 방향 감각 능력과 이어폰의 하드웨어적 고장 여부를 동시에 판별합니다.

## 🛠 기술적 구현 원리 (Technology Stack)

이 앱의 핵심은 미리 녹음된 MP3 파일을 재생하는 것이 아니라, **코드로 실시간 음파를 그려내는 것**입니다.

### 1. AudioTrack API (Low-level Audio)

안드로이드의 고수준 API(MediaPlayer)는 정밀한 제어가 어렵기 때문에, 저수준 API인 AudioTrack을 사용하여 PCM(Pulse Code Modulation) 데이터를 직접 스트리밍했습니다.

### 2. 사인파(Sine Wave) 알고리즘

특정 주파수의 순음(Pure Tone)을 만들기 위해 삼각함수를 사용하여 디지털 신호를 생성합니다.

```kotlin
// [CODE] 실시간 주파수 생성 로직
val sample = DoubleArray(numSamples)
for (i in 0 until numSamples) {
// 2π * 주파수 * 시간 / 샘플레이트 공식 적용
sample[i] = sin(2.0 * Math.PI * i.toDouble() / (sampleRate / frequency))
}
```

3. PCM 16-bit Stereo 인터리빙 (Channel Separation)

좌우 소리를 분리하기 위해 16비트 스테레오 PCM 데이터 구조를 직접 조작했습니다. 배열에 [Left, Right, Left, Right...] 순서로 데이터를 배치하여 채널을 제어합니다.

```kotlin
// [CODE] 스테레오 채널 분리 로직
if (side == "LEFT") {
generatedSnd[idx++] = lowByte; generatedSnd[idx++] = highByte; // Left 채널 데이터
generatedSnd[idx++] = 0;       generatedSnd[idx++] = 0;        // Right 채널 무음 (0)
} else if (side == "RIGHT") {
generatedSnd[idx++] = 0;       generatedSnd[idx++] = 0;        // Left 채널 무음 (0)
generatedSnd[idx++] = lowByte; generatedSnd[idx++] = highByte; // Right 채널 데이터
}
```

## *🚀 설치 및 실행 (How to Run)*

1. 이 저장소(Repository)를 클론합니다.

2. Android Studio에서 프로젝트를 엽니다.

3. 반드시 이어폰을 연결한 상태에서 앱을 빌드하고 실행합니다.

### ⚠️ 주의사항 (Disclaimer)

본 애플리케이션의 결과는 의학적 진단이 아니며 참고용으로만 사용해야 합니다. 청력 이상이 의심될 경우 반드시 전문의와 상담하시기 바랍니다.
