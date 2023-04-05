package com.vd5.beyondb.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.vd5.beyondb.MainActivity
import com.vd5.beyondb.databinding.FragmentHomeBinding

private const val TAG = "HOMEFRAGMENT"

class HomeFragment : Fragment() {

    lateinit var _binding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val textView = _binding.textTutorial
        Handler(Looper.getMainLooper()).postDelayed({
            (activity as MainActivity).TTSrun(textView.text.toString())
        }, 200)
        val tutorialStartButton = _binding.buttonTutorial

        tutorialStartButton.setOnClickListener { tutorial() }
        return _binding.root
    }

    private val tutorialTexts = arrayOf(
        "안녕하세요. \nbeyond barrier입니다.",
        "beyond barrier는 시각장애인을 위한 TV화면 해설 서비스입니다.",
        "두 번째 메뉴를 누르면 현재 프로그램에 대한 정보를 알 수 있습니다.",
        "세 번째 메뉴를 누르면 현재 화면에 대한 설명을 들을 수 있습니다.",
        "네 번째 메뉴에서는 \n설정을 변경할 수 있습니다.",
        "그럼 지금부터 beyond barrier를 \n이용해 보세요."
    )
    private var tutorialIndex = 0
    private val ttsId = "tutorialTTS"
    private fun tutorial() {
        (activity as MainActivity).textToSpeech?.stop()
        val textView = _binding.textTutorial
        val button = _binding.buttonTutorial
        (activity as MainActivity).textToSpeech!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(p0: String?) {
                Log.d(TAG, "onStart: TTS시작!!!")
            }
            // 완료 시점 마다 신호
            override fun onDone(utteranceId: String) {
                Log.d(TAG, "onDone: TTS 완료!!!")
                if (tutorialIndex < tutorialTexts.size -1 ) {
                    tutorialIndex++
                    setAnimatedText(textView, tutorialTexts[tutorialIndex])
                } else {
                    tutorialIndex = 0
                }
            }
            override fun onError(p0: String?) {
            }
        })
        setAnimatedText(textView, tutorialTexts[tutorialIndex])
        button.isEnabled = false
    }

    fun setAnimatedText(textView: TextView, newText: String) {
        val fadeOut = AlphaAnimation(1.0f, 0.0f)
        fadeOut.duration = 500
        fadeOut.interpolator = AccelerateInterpolator()

        val fadeIn = AlphaAnimation(0.0f, 1.0f)
        fadeIn.duration = 500
        fadeIn.interpolator = DecelerateInterpolator()

        val animationSet = AnimationSet(false)
        animationSet.addAnimation(fadeOut)

        animationSet.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                textView.text = newText
                textView.startAnimation(fadeIn)
                (activity as MainActivity).TTSrun(newText, ttsId)
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })

        textView.startAnimation(animationSet)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as MainActivity).textToSpeech?.stop()
    }

}