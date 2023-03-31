package com.vd5.beyondb.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.vd5.beyondb.MainActivity
import com.vd5.beyondb.R



// https://velog.io/@changhee09/%EC%95%88%EB%93%9C%EB%A1%9C%EC%9D%B4%EB%93%9C-%EC%95%B1%EC%9D%98-%ED%99%98%EA%B2%BD%EC%84%A4%EC%A0%95-Preference#6-%EA%B9%83%ED%97%88%EB%B8%8C-%EC%BD%94%EB%93%9C
// https://github.com/dhtmaks2540/PreferenceExample/blob/main/app/src/main/java/kr/co/lee/preferenceexapmle/SettingPreferenceFragment.kt
class SettingsFragment :  PreferenceFragmentCompat() {

    lateinit var prefs: SharedPreferences

    // Preference 객체
    var checkBoxPreference: Preference? = null


    // onCreate() 중에 호출되어 Fragment에 preference를 제공하는 메서드
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // preference xml을 inflate하는 메서드
        setPreferencesFromResource(R.xml.main_preference, rootKey)

        // rootKey가 null 이라면
        if (rootKey == null) {
            // Preference 객체 초기화

            //checkBoxPreference = findPreference("nickname_flag")

            // SharedPreferences 객체 초기화
            prefs = PreferenceManager.getDefaultSharedPreferences(this.requireContext())
        }
    }

    // 설정 변경 이벤트 처리
    private val prefListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences: SharedPreferences?, key: String? ->
            when (key) {
                "tts_speed" -> {
                    val value = prefs.getString("tts_speed","1.0")
                    (activity as MainActivity).textToSpeech?.setSpeechRate(value!!.toFloat())
                }
                "font_size" -> {
                    val value = prefs.getString("font_size","14")
                }
                "captioning_interval" -> {
                    val value = prefs.getString("captioning_interval","7")
                }
            }
        }



    override fun onResume() {
        super.onResume()
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onPause() {
        super.onPause()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
    }
}